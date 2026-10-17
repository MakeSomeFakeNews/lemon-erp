<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-segmented v-model:value="module" :options="moduleOptions" />
        <template #extra>
          <span class="hint">{{ $t('system.codeRule.hint') }}</span>
        </template>
      </TableToolbar>

      <a-table
        row-key="key"
        :size="size"
        :columns="visibleColumns"
        :data-source="filtered"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: 900, y: '100%' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <span>{{ record.name }}</span>
            <a-tag v-if="record.customized" color="blue" :bordered="false" class="custom-tag">{{ $t('system.config.modified') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'example'">
            <span class="example">{{ record.example }}</span>
          </template>
          <template v-else-if="column.key === 'dateFormat'">{{ dateFormatLabel(record.dateFormat) }}</template>
          <template v-else-if="column.key === 'resetCycle'">{{ resetCycleLabel(record.resetCycle) }}</template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button v-perm="'sys:codeRule:edit'" type="link" size="small" @click="drawer.openEdit({ ...record, id: record.key })">
                <template #icon><EditOutlined /></template>{{ $t('common.action.modify') }}
              </a-button>
              <span v-if="record.customized" v-perm="'sys:codeRule:edit'">
                <a-popconfirm :title="$t('system.codeRule.confirmReset', { name: record.name })" @confirm="reset(record)">
                  <a-button type="link" size="small"><template #icon><UndoOutlined /></template>{{ $t('system.config.restoreDefault') }}</a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" :edit-title="$t('system.codeRule.editTitle', { name: form.name || '' })">
      <a-alert
        type="info"
        show-icon
        class="form-tip"
        :message="$t('system.codeRule.formTip')"
      />
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item :label="$t('system.codeRule.prefix')" name="prefix" :extra="$t('system.codeRule.prefixTip')">
          <a-input v-model:value="form.prefix" :maxlength="8" @update:value="(v) => (form.prefix = (v || '').toUpperCase())" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('system.codeRule.dateFormat')" name="dateFormat">
              <a-select v-model:value="form.dateFormat" :options="DATE_FORMATS" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('system.codeRule.resetCycleLabel')" name="resetCycle">
              <a-select v-model:value="form.resetCycle" :options="RESET_CYCLES" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('system.codeRule.seqLength')" name="seqLength" :extra="$t('system.codeRule.seqLengthTip')">
              <a-input-number v-model:value="form.seqLength" :min="3" :max="8" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <div class="preview">
          <div class="preview-label">{{ $t('system.codeRule.preview') }}</div>
          <div class="preview-value">{{ preview }}</div>
          <div v-if="!dateSupportsCycle" class="preview-warn">
            {{ $t('system.codeRule.previewWarn', { cycle: resetCycleLabel(form.resetCycle) }) }}
          </div>
        </div>
      </a-form>
    </FormDrawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import dayjs from 'dayjs'
import { EditOutlined, UndoOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import { listCodeRules, resetCodeRule, updateCodeRule } from '@/api/system/codeRule'
import { useColumns } from '@/composables/useColumns'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useAction } from '@/composables/useAction'

/** 与后端 DocNoCatalog.DateFormat 一致；precision 为日期精确到的周期（越小越精确） */
const { t } = useI18n()
const dateFormat = (value, name, sample, precision) => ({
  value,
  name,
  label: sample ? t('system.codeRule.dateSample', { name, sample }) : name,
  pattern: value === 'NONE' ? '' : value,
  precision
})
const DATE_FORMATS = [
  dateFormat('YYYYMMDD', t('system.codeRule.fmtYmd'), '20261015', 0),
  dateFormat('YYMMDD', t('system.codeRule.fmtShortYmd'), '261015', 0),
  dateFormat('YYYYMM', t('system.codeRule.fmtYm'), '202610', 1),
  dateFormat('YYMM', t('system.codeRule.fmtShortYm'), '2610', 1),
  dateFormat('YYYY', t('system.codeRule.fmtY'), '2026', 2),
  dateFormat('NONE', t('system.codeRule.fmtNone'), '', 3)
]
const RESET_CYCLES = [
  { value: 'DAY', label: t('system.codeRule.cycleDay'), order: 0 },
  { value: 'MONTH', label: t('system.codeRule.cycleMonth'), order: 1 },
  { value: 'YEAR', label: t('system.codeRule.cycleYear'), order: 2 },
  { value: 'NEVER', label: t('system.codeRule.cycleNever'), order: 3 }
]
const dateFormatLabel = (v) => DATE_FORMATS.find((d) => d.value === v)?.name ?? v
const resetCycleLabel = (v) => RESET_CYCLES.find((d) => d.value === v)?.label ?? v

const columns = [
  { title: t('system.codeRule.doc'), key: 'name', width: 200 },
  { title: t('system.codeRule.module'), dataIndex: 'module', width: 120 },
  { title: t('system.codeRule.example'), key: 'example', width: 200 },
  { title: t('system.codeRule.prefix'), dataIndex: 'prefix', width: 90 },
  { title: t('system.codeRule.dateFormat'), key: 'dateFormat', width: 130 },
  { title: t('system.codeRule.seqLength'), dataIndex: 'seqLength', width: 110, align: 'right' },
  { title: t('system.codeRule.resetCycle'), key: 'resetCycle', width: 110 },
  { title: t('common.action.operation'), key: 'action', width: 220, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'system-code-rule')
const size = ref('middle')
const list = ref([])
const loading = ref(false)
// 全部模块用空字符串表示
const module = ref('')
const moduleOptions = computed(() => [
  { label: t('common.status.all'), value: '' },
  ...[...new Set(list.value.map((r) => r.module))].map((m) => ({ label: m, value: m }))
])
const filtered = computed(() => (module.value === '' ? list.value : list.value.filter((r) => r.module === module.value)))

async function load() {
  loading.value = true
  try {
    list.value = await listCodeRules()
  } finally {
    loading.value = false
  }
}
onMounted(load)

const rules = {
  prefix: [
    { required: true, message: t('system.codeRule.inputPrefix') },
    { pattern: /^[A-Z0-9-]{1,8}$/, message: t('system.codeRule.prefixPattern') }
  ],
  dateFormat: [{ required: true, message: t('system.codeRule.selectDateFormat') }],
  resetCycle: [{ required: true, message: t('system.codeRule.selectResetCycle') }],
  seqLength: [{ required: true, message: t('system.codeRule.inputSeqLength') }]
}
const drawer = useFormDrawer({
  emptyForm: () => ({ key: undefined, name: '', prefix: '', dateFormat: 'YYYYMMDD', seqLength: 4, resetCycle: 'DAY' }),
  create: () => Promise.resolve(),
  update: (key, form) => updateCodeRule(key, form),
  toPayload: ({ prefix, dateFormat, seqLength, resetCycle }) => ({ prefix, dateFormat, seqLength, resetCycle }),
  onSuccess: load
})
// useFormDrawer 以 id 区分新增和修改，编码规则以 key 作为 id 打开
const { formRef, form } = drawer

const dateSupportsCycle = computed(() => {
  const d = DATE_FORMATS.find((x) => x.value === form.value.dateFormat)
  const c = RESET_CYCLES.find((x) => x.value === form.value.resetCycle)
  return !d || !c || d.precision <= c.order
})
const preview = computed(() => {
  const d = DATE_FORMATS.find((x) => x.value === form.value.dateFormat)
  const date = d?.pattern ? dayjs().format(d.pattern) : ''
  return `${form.value.prefix || ''}${date}${'1'.padStart(form.value.seqLength || 4, '0')}`
})

const reset = useAction((record) => resetCodeRule(record.key), { successKey: 'system.codeRule.resetDone', reload: () => load() })
</script>

<style scoped>
.hint {
  color: var(--app-text-3);
  font-size: 12px;
}
.custom-tag {
  margin-left: 6px;
}
.example {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}
.form-tip {
  margin-bottom: 16px;
}
.full {
  width: 100%;
}
.preview {
  padding: 12px 16px;
  border-radius: 8px;
  background: var(--app-hover);
}
.preview-label {
  font-size: 12px;
  color: var(--app-text-3);
}
.preview-value {
  margin-top: 4px;
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 1px;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  color: var(--app-text);
}
.preview-warn {
  margin-top: 6px;
  font-size: 12px;
  color: var(--status-critical);
}
</style>
