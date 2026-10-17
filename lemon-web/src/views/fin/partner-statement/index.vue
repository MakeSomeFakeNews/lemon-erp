<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <div class="filters">
        <a-radio-group v-model:value="kind" button-style="solid" @change="onKindChange">
          <a-radio-button value="AR">{{ $t('fin.partnerStatement.arKind') }}</a-radio-button>
          <a-radio-button value="AP">{{ $t('fin.partnerStatement.apKind') }}</a-radio-button>
        </a-radio-group>
        <PartnerSelect
          :key="kind"
          v-model:value="partnerId"
          :type="kind === 'AR' ? 'customer' : 'supplier'"
          :placeholder="kind === 'AR' ? $t('fin.partnerStatement.selectCustomer') : $t('fin.partnerStatement.selectSupplier')"
          class="partner"
        />
        <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" :allow-clear="false" />
        <a-button type="primary" :disabled="!partnerId" @click="load">{{ $t('common.action.query') }}</a-button>
        <a-button :disabled="!data" @click="print"><template #icon><PrinterOutlined /></template>{{ $t('common.action.print') }}</a-button>
        <span class="sub-text">{{ kind === 'AR' ? $t('fin.partnerStatement.arTip') : $t('fin.partnerStatement.apTip') }}</span>
      </div>

      <template v-if="data">
        <a-row :gutter="12" class="cards">
          <a-col :xs="12" :md="6"><a-statistic :title="$t('fin.partnerStatement.opening')" :value="Number(data.opening)" :precision="2" prefix="¥" /></a-col>
          <a-col :xs="12" :md="6"><a-statistic :title="labels.periodIncrease" :value="Number(data.increase)" :precision="2" prefix="¥" /></a-col>
          <a-col :xs="12" :md="6"><a-statistic :title="labels.periodDecrease" :value="Number(data.decrease)" :precision="2" prefix="¥" /></a-col>
          <a-col :xs="12" :md="6"><a-statistic :title="$t('fin.partnerStatement.closing')" :value="Number(data.closing)" :precision="2" prefix="¥" /></a-col>
        </a-row>
      </template>

      <a-table
        :row-key="(r, i) => i"
        size="small"
        bordered
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        :pagination="false"
        :row-class-name="(r) => (r.type === 'OPENING' ? 'total-row' : '')"
        :locale="{ emptyText: partnerId ? $t('common.msg.noData') : kind === 'AR' ? $t('fin.partnerStatement.selectCustomerFirst') : $t('fin.partnerStatement.selectSupplierFirst') }"
        :scroll="{ x: 1100, y: '100%' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'type'">{{ typeText(record) }}</template>
          <template v-else-if="column.key === 'source'">{{ sourceText(record) }}</template>
          <template v-else-if="['increase', 'decrease'].includes(column.key)">
            <span :class="{ neg: Number(record[column.key]) < 0 }">{{ moneyOrBlank(record[column.key]) }}</span>
          </template>
          <template v-else-if="column.key === 'balance'">
            <span :class="['bold', { neg: Number(record.balance) < 0 }]">{{ formatMoney(record.balance) }}</span>
          </template>
        </template>
        <template v-if="data" #summary>
          <a-table-summary fixed>
            <a-table-summary-row class="total-row">
              <a-table-summary-cell :col-span="4">{{ $t('fin.partnerStatement.periodTotal') }}</a-table-summary-cell>
              <a-table-summary-cell align="right">{{ formatMoney(data.increase) }}</a-table-summary-cell>
              <a-table-summary-cell align="right">{{ formatMoney(data.decrease) }}</a-table-summary-cell>
              <a-table-summary-cell align="right">{{ formatMoney(data.closing) }}</a-table-summary-cell>
              <a-table-summary-cell />
            </a-table-summary-row>
          </a-table-summary>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import dayjs from 'dayjs'
import { useI18n } from 'vue-i18n'
import { PrinterOutlined } from '@ant-design/icons-vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import { partnerStatement } from '@/api/fin'
import { formatMoney, moneyOrBlank } from '@/components/fin/fin'
import { useDict } from '@/composables/useDict'
import { findDict } from '@/utils/dict'
import { printDocs } from '@/utils/print'

/** 往来对账单：客户 / 供应商在期间内的应收应付、收付款、预收预付和逐笔余额，可打印给对方核对 */
const { t } = useI18n()
const dict = useDict('fin_bill_source', 'fin_settle_method')
const kind = ref('AR')
const partnerId = ref()
const range = ref([dayjs().startOf('year').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')])
const data = ref()
const loading = ref(false)

const labels = computed(() =>
  kind.value === 'AR'
    ? {
        partner: t('fin.partnerStatement.customer'),
        billAmount: t('fin.partnerStatement.arAmount'),
        settleAmount: t('fin.partnerStatement.receiptAmount'),
        bill: t('fin.partnerStatement.arBill'),
        settle: t('fin.partnerStatement.receipt'),
        advance: t('fin.partnerStatement.advanceReceipt'),
        periodIncrease: t('fin.partnerStatement.periodAr'),
        periodDecrease: t('fin.partnerStatement.periodReceipt'),
        title: t('fin.partnerStatement.customerTitle'),
        confirm: t('fin.partnerStatement.customerConfirm')
      }
    : {
        partner: t('fin.partnerStatement.supplier'),
        billAmount: t('fin.partnerStatement.apAmount'),
        settleAmount: t('fin.partnerStatement.paymentAmount'),
        bill: t('fin.partnerStatement.apBill'),
        settle: t('fin.partnerStatement.payment'),
        advance: t('fin.partnerStatement.advancePayment'),
        periodIncrease: t('fin.partnerStatement.periodAp'),
        periodDecrease: t('fin.partnerStatement.periodPayment'),
        title: t('fin.partnerStatement.supplierTitle'),
        confirm: t('fin.partnerStatement.supplierConfirm')
      }
)
const columns = computed(() => [
  { title: t('common.field.date'), dataIndex: 'bizDate', width: 110 },
  { title: t('common.field.type'), key: 'type', width: 130 },
  { title: t('fin.partnerStatement.docNo'), dataIndex: 'docNo', width: 160 },
  { title: t('fin.partnerStatement.source'), key: 'source', width: 200, ellipsis: true },
  { title: labels.value.billAmount, key: 'increase', width: 150, align: 'right' },
  { title: labels.value.settleAmount, key: 'decrease', width: 150, align: 'right' },
  { title: t('fin.partnerStatement.balance'), key: 'balance', width: 140, align: 'right' },
  { title: t('common.field.remark'), dataIndex: 'remark', ellipsis: true }
])
const rows = computed(() =>
  data.value ? [{ type: 'OPENING', bizDate: range.value[0], balance: data.value.opening }, ...data.value.rows] : []
)

function typeText(r) {
  const l = labels.value
  return { OPENING: t('fin.partnerStatement.opening'), BILL: l.bill, SETTLEMENT: l.settle, ADVANCE: l.advance }[r.type]
}
function sourceText(r) {
  if (r.type === 'OPENING') return ''
  if (r.type === 'BILL') {
    const src = findDict(dict.fin_bill_source, r.sourceType)?.label || r.sourceType || ''
    return [src, r.sourceNo].filter(Boolean).join(' ')
  }
  return findDict(dict.fin_settle_method, r.sourceType)?.label || r.sourceType || ''
}

let seq = 0
async function load() {
  if (!partnerId.value) return
  const current = ++seq
  loading.value = true
  try {
    const res = await partnerStatement({ kind: kind.value, partnerId: partnerId.value, begin: range.value[0], end: range.value[1] })
    if (current === seq) data.value = res
  } finally {
    if (current === seq) loading.value = false
  }
}
function onKindChange() {
  partnerId.value = undefined
  data.value = undefined
}
watch([partnerId, range], load)

function print() {
  const d = data.value
  const l = labels.value
  printDocs({
    title: l.title,
    fields: [
      [l.partner, d.partnerName],
      [t('fin.partnerStatement.statementPeriod'), t('fin.partnerStatement.range', { begin: range.value[0], end: range.value[1] })],
      [t('fin.partnerStatement.opening'), formatMoney(d.opening)],
      [t('fin.partnerStatement.closing'), formatMoney(d.closing)]
    ],
    columns: [
      { title: t('common.field.date'), key: 'bizDate' },
      { title: t('common.field.type'), key: 'type', value: typeText },
      { title: t('fin.partnerStatement.docNo'), key: 'docNo' },
      { title: t('fin.partnerStatement.source'), key: 'source', value: sourceText },
      { title: t('fin.partnerStatement.increase'), key: 'increase', value: (r) => moneyOrBlank(r.increase), align: 'right' },
      { title: t('fin.partnerStatement.decrease'), key: 'decrease', value: (r) => moneyOrBlank(r.decrease), align: 'right' },
      { title: t('fin.partnerStatement.balance'), key: 'balance', value: (r) => formatMoney(r.balance), align: 'right' },
      { title: t('common.field.remark'), key: 'remark' }
    ],
    rows: rows.value,
    totals: { bizDate: t('fin.partnerStatement.periodTotal'), increase: formatMoney(d.increase), decrease: formatMoney(d.decrease), balance: formatMoney(d.closing) },
    signs: [t('fin.partnerStatement.preparer'), t('fin.partnerStatement.financeReview'), l.confirm],
    note: t('fin.partnerStatement.note')
  }, `${l.title} ${d.partnerName}`, 'fin.partnerStatement')
}
</script>

<style scoped>
.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
  margin-bottom: 12px;
}
.partner {
  width: 240px;
}
.cards {
  margin-bottom: 12px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.neg {
  color: var(--status-critical);
}
.bold {
  font-weight: 500;
}
:deep(.total-row) td {
  font-weight: 500;
  background: var(--app-bg-2, #fafafa);
}
</style>
