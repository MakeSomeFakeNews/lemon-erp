<template>
  <div class="ai-import-mapper">
    <a-spin :spinning="loading" :tip="$t('ai.importMap.mapping')">
      <a-alert type="info" show-icon :message="$t('ai.importMap.tip')" class="tip" />
      <a-table row-key="header" size="small" :columns="tableColumns" :data-source="rows" :pagination="false" :scroll="{ y: 300 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'header'">
            <div class="header">{{ record.header }}</div>
            <div v-if="record.reason" class="sub-text reason">{{ record.reason }}</div>
          </template>
          <template v-else-if="column.key === 'samples'">
            <span class="sub-text">{{ record.samples.join(' / ') || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'key'">
            <a-select v-model:value="record.key" class="field-select" :options="fieldOptions" />
          </template>
        </template>
      </a-table>
      <a-alert v-if="duplicate" type="error" show-icon class="after" :message="$t('ai.importMap.duplicateField', { field: duplicate })" />
      <a-alert v-else-if="missing.length" type="warning" show-icon class="after" :message="$t('ai.importMap.missing', { fields: missing.join(', ') })" />
      <div class="actions">
        <a-button @click="emit('cancel')">{{ $t('common.action.cancel') }}</a-button>
        <a-button type="primary" :disabled="loading || !!duplicate || !!missing.length" @click="apply">{{ $t('ai.importMap.apply') }}</a-button>
      </div>
    </a-spin>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { mapImportColumns } from '@/api/ai/cleanse'

/**
 * 导入字段对应（ImportModal 中使用）：把 Excel 表头和前几行发给 AI 得到「Excel 列 → 模板字段」，用户核对、调整后 emit('apply', 字段 → 表头)
 */
const props = defineProps({
  /** 模板列（ImportModal 的 columns） */
  columns: { type: Array, required: true },
  headers: { type: Array, required: true },
  samples: { type: Array, default: () => [] }
})
const emit = defineEmits(['apply', 'cancel'])
const { t } = useI18n()

const IGNORE = ''
const loading = ref(false)
const rows = ref(
  props.headers.map((header, i) => ({
    header,
    samples: props.samples.map((r) => r[i]).filter(Boolean).slice(0, 2),
    key: IGNORE,
    reason: null
  }))
)
const tableColumns = [
  { title: t('ai.importMap.excelColumn'), key: 'header', width: 200 },
  { title: t('ai.importMap.samples'), key: 'samples' },
  { title: t('ai.importMap.templateField'), key: 'key', width: 200 }
]
const fieldOptions = computed(() => [
  { value: IGNORE, label: t('ai.importMap.ignore') },
  ...props.columns.map((c) => ({ value: c.key, label: (c.required ? '* ' : '') + c.title }))
])
const duplicate = computed(() => {
  const seen = new Set()
  for (const r of rows.value) {
    if (r.key === IGNORE) continue
    if (seen.has(r.key)) return props.columns.find((c) => c.key === r.key)?.title
    seen.add(r.key)
  }
  return null
})
const missing = computed(() => {
  const used = new Set(rows.value.map((r) => r.key))
  return props.columns.filter((c) => c.required && !used.has(c.key)).map((c) => c.title)
})

function apply() {
  const map = {}
  rows.value.filter((r) => r.key !== IGNORE).forEach((r) => (map[r.key] = r.header))
  emit('apply', map)
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await mapImportColumns({
      columns: props.columns.map((c) => ({ key: c.key, title: c.title, required: !!c.required })),
      headers: props.headers,
      samples: props.samples
    })
    const byHeader = new Map(res.mappings.map((m) => [m.header, m]))
    rows.value.forEach((r) => {
      const m = byHeader.get(r.header)
      if (m) {
        r.key = m.key
        r.reason = m.reason
      }
    })
  } catch {
    emit('cancel')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.tip {
  margin-bottom: 12px;
}
.after {
  margin-top: 12px;
}
.header {
  font-weight: 500;
}
.reason {
  font-size: 12px;
}
.field-select {
  width: 100%;
}
.actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}
</style>
