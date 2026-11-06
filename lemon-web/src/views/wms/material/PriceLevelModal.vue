<template>
  <a-modal :open="open" :title="$t('pricing.level.title')" :confirm-loading="saving" width="min(560px, 100vw)" @ok="save" @cancel="open = false">
    <a-alert type="info" show-icon class="tip" :message="$t('pricing.level.tip')" />
    <a-table row-key="level" size="small" :columns="columns" :data-source="rows" :pagination="false">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'level'">{{ record.level }}</template>
        <template v-else-if="column.key === 'name'">
          <a-input v-model:value="record.name" :maxlength="32" :status="record.name?.trim() ? '' : 'error'" />
        </template>
        <template v-else-if="column.key === 'status'">
          <a-switch :checked="record.status === STATUS_ENABLED" @change="(v) => (record.status = v ? STATUS_ENABLED : STATUS_DISABLED)" />
        </template>
      </template>
    </a-table>
  </a-modal>
</template>

<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { savePriceLevels } from '@/api/scm/pricing'
import { reloadPriceLevels } from '@/components/wms/priceLevels'
import { STATUS_DISABLED, STATUS_ENABLED } from '@/utils/constants'

/** 价格列设置：零售价、批发价1-8 的显示名称和是否启用；保存后触发 saved */
const emit = defineEmits(['saved'])
const { t } = useI18n()
const open = ref(false)
const saving = ref(false)
const rows = ref([])
const columns = [
  { title: t('pricing.level.level'), key: 'level', width: 80 },
  { title: t('pricing.level.name'), key: 'name' },
  { title: t('pricing.level.enabled'), key: 'status', width: 90 }
]

async function show() {
  const list = await reloadPriceLevels()
  rows.value = list.map(({ level, name, status }) => ({ level, name, status }))
  open.value = true
}

async function save() {
  if (rows.value.some((r) => !r.name?.trim())) {
    message.warning(t('pricing.level.nameRequired'))
    return
  }
  saving.value = true
  try {
    await savePriceLevels(rows.value.map((r) => ({ ...r, name: r.name.trim() })))
    await reloadPriceLevels()
    message.success(t('pricing.level.saved'))
    open.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}

defineExpose({ open: show })
</script>

<style scoped>
.tip {
  margin-bottom: 12px;
}
</style>
