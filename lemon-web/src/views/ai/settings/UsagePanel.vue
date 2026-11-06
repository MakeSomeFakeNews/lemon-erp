<template>
  <a-card :bordered="false" class="page-card usage-card" :title="$t('ai.settings.usage')" :loading="loading">
    <template #extra>
      <span class="sub-text">{{ usage.month }}</span>
    </template>
    <div class="total">
      <div>
        <div class="sub-text">{{ $t('ai.settings.usageTotal') }}</div>
        <div class="total-num">{{ num(usage.totalTokens) }}</div>
      </div>
      <div class="sub-text">{{ usage.limit ? $t('ai.settings.usageLimit', { limit: num(usage.limit) }) : $t('ai.settings.unlimited') }}</div>
    </div>
    <a-progress v-if="usage.limit" :percent="percent" :status="percent >= 100 ? 'exception' : 'normal'" :show-info="false" />

    <div class="block-title">{{ $t('ai.settings.byFeature') }}</div>
    <a-table :columns="featureColumns" :data-source="usage.byFeature" row-key="key" size="small" :pagination="false">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'key'">{{ $t(`ai.feature.${record.key}`) }}</template>
        <template v-else-if="column.key === 'total'">{{ num(record.promptTokens + record.completionTokens) }}</template>
        <template v-else-if="column.dataIndex">{{ num(record[column.dataIndex]) }}</template>
      </template>
      <template #emptyText>{{ $t('ai.settings.noUsage') }}</template>
    </a-table>

    <div class="block-title">{{ $t('ai.settings.byUser') }}</div>
    <a-table :columns="userColumns" :data-source="usage.byUser" row-key="key" size="small" :pagination="false">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'total'">{{ num(record.promptTokens + record.completionTokens) }}</template>
        <template v-else-if="column.key === 'calls'">{{ num(record.calls) }}</template>
      </template>
      <template #emptyText>{{ $t('ai.settings.noUsage') }}</template>
    </a-table>
  </a-card>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { getAiUsage } from '@/api/ai/config'
import { num } from '@/utils/number'

/** 本月用量：合计与上限、按功能、按人员 */
const { t } = useI18n()
const loading = ref(false)
const usage = ref({ byFeature: [], byUser: [] })
const percent = computed(() => (usage.value.limit ? Math.min(100, Math.round((usage.value.totalTokens * 100) / usage.value.limit)) : 0))

const featureColumns = [
  { title: t('ai.settings.byFeature'), key: 'key' },
  { title: t('ai.settings.calls'), dataIndex: 'calls', align: 'right' },
  { title: t('ai.settings.totalTokens'), key: 'total', align: 'right' }
]
const userColumns = [
  { title: t('ai.settings.user'), dataIndex: 'name', ellipsis: true },
  { title: t('ai.settings.calls'), key: 'calls', align: 'right' },
  { title: t('ai.settings.totalTokens'), key: 'total', align: 'right' }
]

async function reload() {
  loading.value = true
  try {
    usage.value = await getAiUsage()
  } finally {
    loading.value = false
  }
}

defineExpose({ reload })
onMounted(reload)
</script>

<style scoped>
.usage-card {
  margin: 0;
}
.total {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 4px;
}
.total-num {
  font-size: 26px;
  font-weight: 600;
  line-height: 1.3;
}
.block-title {
  font-weight: 600;
  margin: 16px 0 8px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
