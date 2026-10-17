<template>
  <a-table row-key="id" size="small" :columns="columns" :data-source="reports" :pagination="false">
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'op'">{{ record.opSeq }} {{ record.opName }}</template>
      <template v-else-if="['goodQty', 'scrapQty', 'hours'].includes(column.key)">{{ formatQty(record[column.key]) }}</template>
      <template v-else-if="column.key === 'operator'">{{ record.operatorName || record.createByName }}</template>
      <template v-else-if="column.key === 'pieceWage'">{{ record.pieceWage != null ? '¥' + formatQty(record.pieceWage) : '-' }}</template>
      <template v-else-if="column.key === 'equipment'">
        {{ record.equipmentName || '-' }}
        <div v-if="record.moldCode" class="sub-text">{{ $t('mes.wo.moldShots', { code: record.moldCode, shots: record.shots }) }}</div>
      </template>
    </template>
  </a-table>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { formatQty } from '@/components/wms/wms'

/** 工单详情 - 报工记录 */
defineProps({ reports: { type: Array, default: () => [] } })
const { t } = useI18n()

const columns = [
  { title: t('common.field.date'), dataIndex: 'reportDate', width: 110 },
  { title: t('mes.wo.op'), key: 'op', width: 140 },
  { title: t('mes.wo.good'), key: 'goodQty', width: 80, align: 'right' },
  { title: t('mes.wo.scrap'), key: 'scrapQty', width: 80, align: 'right' },
  { title: t('mes.wo.hours'), key: 'hours', width: 80, align: 'right' },
  { title: t('mes.wo.operator'), key: 'operator', width: 100 },
  { title: t('mes.wo.pieceWage'), key: 'pieceWage', width: 100, align: 'right' },
  { title: t('mes.wo.equipmentMold'), key: 'equipment', width: 160 },
  { title: t('common.field.remark'), dataIndex: 'remark', ellipsis: true }
]
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
