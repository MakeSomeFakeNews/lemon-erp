<template>
  <div class="filters">
    <a-radio-group v-model:value="kindModel" button-style="solid" @change="loadReport">
      <a-radio-button :value="InvoiceKind.OUTPUT">{{ $t('fin.invoice.reportOutput') }}</a-radio-button>
      <a-radio-button :value="InvoiceKind.INPUT">{{ $t('fin.invoice.reportInput') }}</a-radio-button>
    </a-radio-group>
    <span>{{ $t('fin.invoice.asOf') }}</span>
    <a-date-picker v-model:value="asOfModel" value-format="YYYY-MM-DD" :allow-clear="false" @change="loadReport" />
    <span class="sub-text">
      {{ kindModel === InvoiceKind.OUTPUT ? $t('fin.invoice.reportOutputTip') : $t('fin.invoice.reportInputTip') }}
    </span>
  </div>
  <div class="stats">
    <span>{{ kindModel === InvoiceKind.OUTPUT ? $t('fin.invoice.customerCount', { n: report.length }) : $t('fin.invoice.supplierCount', { n: report.length }) }}</span>
    <span>{{ $t('fin.invoice.billAmount') }} ¥{{ formatMoney(reportTotal.amount) }}</span>
    <span>{{ kindModel === InvoiceKind.OUTPUT ? $t('fin.invoice.invoicedOut') : $t('fin.invoice.invoicedIn') }} ¥{{ formatMoney(reportTotal.invoiced) }}</span>
    <span>{{ kindModel === InvoiceKind.OUTPUT ? $t('fin.invoice.uninvoicedOut') : $t('fin.invoice.estimated') }} <b class="neg">¥{{ formatMoney(reportTotal.uninvoiced) }}</b></span>
    <span>{{ $t('fin.invoice.ofWhichTax') }} ¥{{ formatMoney(reportTotal.uninvoicedTax) }}</span>
  </div>
  <a-table
    row-key="partnerId"
    size="middle"
    :columns="reportColumns"
    :data-source="report"
    :loading="reportLoading"
    :pagination="report.length > 20 ? { pageSize: 20, showTotal: (total) => $t('common.table.total', { total }) } : false"
    :scroll="{ x: 900 }"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="['amount', 'invoiced', 'uninvoiced', 'uninvoicedTax'].includes(column.key)">
        <span :class="{ bold: column.key === 'uninvoiced' }">{{ formatMoney(record[column.key]) }}</span>
      </template>
    </template>
  </a-table>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { uninvoiced } from '@/api/fin'
import { formatMoney } from '@/components/fin/fin'
import { InvoiceKind } from '@/constants/enums'

/** 未开票 / 暂估：按往来单位汇总截止日未开票的单据。种类、截止日由页面保存（v-model:report-kind、v-model:as-of），切换页签后保持 */
const props = defineProps({ reportKind: { type: String, required: true }, asOf: { type: String, required: true } })
const emit = defineEmits(['update:reportKind', 'update:asOf'])
const { t } = useI18n()

const kindModel = ref(props.reportKind)
const asOfModel = ref(props.asOf)
watch(kindModel, (v) => emit('update:reportKind', v))
watch(asOfModel, (v) => emit('update:asOf', v))
const report = ref([])
const reportLoading = ref(false)
const reportColumns = computed(() => [
  { title: kindModel.value === InvoiceKind.OUTPUT ? t('fin.invoice.customer') : t('fin.invoice.supplier'), dataIndex: 'partnerName', width: 240, ellipsis: true },
  { title: t('fin.invoice.billCount'), dataIndex: 'billCount', width: 100, align: 'right' },
  { title: t('fin.invoice.billAmount'), key: 'amount', width: 140, align: 'right' },
  { title: kindModel.value === InvoiceKind.OUTPUT ? t('fin.invoice.invoicedOut') : t('fin.invoice.invoicedIn'), key: 'invoiced', width: 140, align: 'right' },
  { title: kindModel.value === InvoiceKind.OUTPUT ? t('fin.invoice.uninvoicedOut') : t('fin.invoice.estimatedAmount'), key: 'uninvoiced', width: 150, align: 'right' },
  { title: t('fin.invoice.ofWhichTax'), key: 'uninvoicedTax', width: 140, align: 'right' }
])
const reportTotal = computed(() =>
  report.value.reduce(
    (t, r) => ({
      amount: t.amount + Number(r.amount || 0),
      invoiced: t.invoiced + Number(r.invoiced || 0),
      uninvoiced: t.uninvoiced + Number(r.uninvoiced || 0),
      uninvoicedTax: t.uninvoicedTax + Number(r.uninvoicedTax || 0)
    }),
    { amount: 0, invoiced: 0, uninvoiced: 0, uninvoicedTax: 0 }
  )
)
async function loadReport() {
  reportLoading.value = true
  try {
    report.value = await uninvoiced(kindModel.value, asOfModel.value)
  } finally {
    reportLoading.value = false
  }
}
onMounted(loadReport)
</script>

<style scoped>
.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.stats {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  margin-bottom: 8px;
  color: var(--app-text-2);
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.neg {
  color: #cf1322;
}
.bold {
  font-weight: 600;
}
</style>
