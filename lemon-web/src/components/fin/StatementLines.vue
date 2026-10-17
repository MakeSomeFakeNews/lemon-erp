<template>
  <a-table row-key="id" size="small" :columns="columns" :data-source="lines" :pagination="false" :scroll="{ x: 700 }">
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'source'">
        <DictTag type="fin_bill_source" :value="record.sourceType" />
        <span class="source-no">{{ record.sourceNo }}</span>
      </template>
      <template v-else-if="column.key === 'amount'">
        <span :class="{ negative: Number(record.amount) < 0 }">¥{{ formatMoney(record.amount) }}</span>
      </template>
      <template v-else-if="column.key === 'tax'">¥{{ formatMoney(record.taxAmount) }}</template>
    </template>
    <template #summary>
      <a-table-summary-row>
        <a-table-summary-cell :col-span="3">{{ $t('fin.statementLines.totalCount', { n: lines.length }) }}</a-table-summary-cell>
        <a-table-summary-cell align="right"><b>¥{{ formatMoney(total) }}</b></a-table-summary-cell>
        <a-table-summary-cell />
      </a-table-summary-row>
    </template>
  </a-table>
</template>

<script setup>
import { computed } from 'vue'
import DictTag from '@/components/DictTag.vue'
import { formatMoney } from './fin'
import { t } from '@/locales'

/** 对账单明细（财务与供应商门户共用）：生成时的应付单快照，退货、索赔为负数 */
const props = defineProps({
  lines: { type: Array, default: () => [] }
})
const columns = [
  { title: t('common.field.date'), dataIndex: 'billDate', width: 110 },
  { title: t('fin.statementLines.billNo'), dataIndex: 'billNo', width: 170 },
  { title: t('fin.statementLines.source'), key: 'source', width: 240 },
  { title: t('fin.statementLines.amount'), key: 'amount', width: 140, align: 'right' },
  { title: t('fin.statementLines.tax'), key: 'tax', width: 120, align: 'right' }
]
const total = computed(() => props.lines.reduce((s, l) => s + Number(l.amount || 0), 0))
</script>

<style scoped>
.source-no {
  margin-left: 4px;
}
.negative {
  color: var(--status-critical);
}
</style>
