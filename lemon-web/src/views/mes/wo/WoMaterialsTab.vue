<template>
  <a-table row-key="id" size="small" :columns="columns" :data-source="materials" :pagination="false">
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'material'">{{ record.materialCode }} {{ record.materialName }}</template>
      <template v-else-if="column.key === 'unitQty'">{{ formatQty(record.unitQty) }}</template>
      <template v-else-if="column.key === 'progress'">
        <a-progress :percent="pct(record.issuedQty, Number(record.requiredQty) + Number(record.supplementQty || 0))" size="small" />
        <div class="sub-text">
          {{ $t('mes.wo.issuedOfRequired', { issued: formatQty(record.issuedQty), req: formatQty(record.requiredQty), unit: record.unit || '' }) }}
          <template v-if="Number(record.supplementQty)">{{ $t('mes.wo.inclSupplement', { v: formatQty(record.supplementQty) }) }}</template>
        </div>
      </template>
    </template>
  </a-table>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { formatQty } from '@/components/wms/wms'
import { pct } from './wo'

/** 工单详情 - 用料 */
defineProps({ materials: { type: Array, default: () => [] } })
const { t } = useI18n()

const columns = [
  { title: t('mes.wo.material'), key: 'material', width: 260 },
  { title: t('mes.wo.unitQty'), key: 'unitQty', width: 170, align: 'right' },
  { title: t('mes.wo.issueProgress'), key: 'progress', width: 260 }
]
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
