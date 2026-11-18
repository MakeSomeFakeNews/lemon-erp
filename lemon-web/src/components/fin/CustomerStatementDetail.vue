<template>
  <div>
    <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
      <a-descriptions-item v-if="!portal" :label="$t('fin.customerStatement.customer')">{{ statement.customerCode }} {{ statement.customerName }}</a-descriptions-item>
      <a-descriptions-item :label="$t('fin.customerStatement.period')">{{ statement.beginDate }} ~ {{ statement.endDate }}</a-descriptions-item>
      <a-descriptions-item :label="$t('common.field.status')"><DictTag type="fin_statement_status" :value="statement.status" /></a-descriptions-item>
      <a-descriptions-item :label="$t('fin.customerStatement.confirmTime')">
        <template v-if="statement.confirmTime">{{ statement.confirmTime }} {{ statement.confirmByName }}</template>
        <span v-else>-</span>
      </a-descriptions-item>
      <a-descriptions-item v-if="!portal" :label="$t('fin.customerStatement.createBy')">{{ statement.createByName }} {{ statement.createTime }}</a-descriptions-item>
      <a-descriptions-item v-if="statement.customerRemark" :label="$t('fin.customerStatement.customerRemark')" :span="3">{{ statement.customerRemark }}</a-descriptions-item>
      <a-descriptions-item v-if="statement.remark" :label="$t('common.field.remark')" :span="3">{{ statement.remark }}</a-descriptions-item>
    </a-descriptions>

    <a-row :gutter="12" class="block">
      <a-col v-for="c in cards" :key="c.key" :xs="12" :sm="8" :md="4">
        <a-statistic :title="c.title" :value="Number(c.value)" :precision="2" prefix="¥" :value-style="c.strong ? { fontWeight: 600 } : undefined" />
      </a-col>
    </a-row>

    <div class="toolbar">
      <span class="section-title">{{ $t('fin.customerStatement.lines', { n: statement.lineCount }) }}</span>
      <a-space>
        <a-button v-if="!portal" :loading="printing" @click="print"><template #icon><PrinterOutlined /></template>{{ $t('common.action.print') }}</a-button>
        <a-button :loading="exporting" @click="exportAll"><template #icon><DownloadOutlined /></template>{{ $t('common.action.export') }}</a-button>
      </a-space>
    </div>
    <a-table
      row-key="id"
      size="small"
      bordered
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="pagination"
      :row-class-name="(r) => ([OPENING, CARRIED].includes(r.type) ? 'total-row' : '')"
      :scroll="{ x: 1100 }"
      @change="onTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'type'">{{ ledgerTypeText(record, labels) }}</template>
        <template v-else-if="column.key === 'source'">{{ ledgerSourceText(record, dict) }}</template>
        <template v-else-if="column.key === 'increase'">
          <span :class="{ neg: Number(record.increase) < 0 }">{{ moneyOrBlank(record.increase) }}</span>
        </template>
        <template v-else-if="column.key === 'decrease'">
          {{ moneyOrBlank(record.decrease) }}
          <div v-if="Number(record.discount)" class="sub-text">{{ $t('fin.partnerStatement.discountOf', { amount: formatMoney(record.discount) }) }}</div>
        </template>
        <template v-else-if="column.key === 'balance'">
          <span :class="['bold', { neg: Number(record.balance) < 0 }]">{{ formatMoney(record.balance) }}</span>
        </template>
      </template>
      <template #summary>
        <a-table-summary-row class="total-row">
          <a-table-summary-cell :col-span="4">{{ $t('fin.partnerStatement.periodTotal') }}</a-table-summary-cell>
          <a-table-summary-cell align="right">{{ formatMoney(totals.increase) }}</a-table-summary-cell>
          <a-table-summary-cell align="right">{{ formatMoney(totals.decrease) }}</a-table-summary-cell>
          <a-table-summary-cell align="right">{{ formatMoney(totals.closing) }}</a-table-summary-cell>
          <a-table-summary-cell />
        </a-table-summary-row>
      </template>
    </a-table>

    <div class="section-title attachments">{{ $t('fin.customerStatement.attachments') }}</div>
    <AttachmentPanel biz-type="fin_customer_statement" :biz-id="statement.id" :editable="attachmentEditable" />
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { DownloadOutlined, PrinterOutlined } from '@ant-design/icons-vue'
import AttachmentPanel from '@/components/AttachmentPanel.vue'
import DictTag from '@/components/DictTag.vue'
import { formatMoney, moneyOrBlank } from './fin'
import {
  CARRIED, OPENING, exportLedger, ledgerColumns, ledgerLabels, ledgerPrintDoc, ledgerSourceText, ledgerTypeText, printField, withOpening
} from './partnerLedger'
import { useDict } from '@/composables/useDict'
import { printDocs } from '@/utils/print'
import { BillKind } from '@/constants/enums'

/**
 * 客户对账单详情（财务与经销商门户共用，issue #134）：期初、本期应收、收款、折让、对冲、期末，明细分页（每行余额为生成时的逐笔余额），
 * 打印（财务）、导出全部明细，客户签字盖章的扫描件等附件
 */
const props = defineProps({
  statement: { type: Object, required: true },
  /** 明细接口：(id, { current, size }) => { records, total }，不传 size 返回全部 */
  linesApi: { type: Function, required: true },
  /** 经销商门户：不显示客户、制单人，不能打印（导出 Excel） */
  portal: { type: Boolean, default: false },
  attachmentEditable: { type: Boolean, default: false }
})
const { t } = useI18n()
const dict = useDict('fin_bill_source', 'fin_settle_method')
const labels = ledgerLabels(BillKind.AR)
const columns = ledgerColumns(labels)

const cards = computed(() => {
  const s = props.statement
  return [
    { key: 'opening', title: t('fin.partnerStatement.opening'), value: s.opening },
    { key: 'bill', title: t('fin.customerStatement.billAmount'), value: s.billAmount },
    { key: 'receipt', title: t('fin.customerStatement.receiptAmount'), value: s.receiptAmount },
    { key: 'discount', title: t('fin.customerStatement.discountAmount'), value: s.discountAmount },
    ...(Number(s.nettingAmount) ? [{ key: 'netting', title: t('fin.customerStatement.nettingAmount'), value: s.nettingAmount }] : []),
    { key: 'closing', title: t('fin.customerStatement.closing'), value: s.closing, strong: true }
  ]
})
const totals = computed(() => {
  const s = props.statement
  const decrease = Number(s.receiptAmount) + Number(s.discountAmount) + Number(s.nettingAmount)
  return { increase: s.billAmount, decrease, closing: s.closing }
})

const lines = ref([])
const loading = ref(false)
const pagination = reactive({
  current: 1,
  pageSize: 50,
  total: 0,
  showSizeChanger: true,
  pageSizeOptions: ['20', '50', '100', '200', '500'],
  showTotal: (total) => t('common.table.total', { total })
})
// 每页首行：第一页为期初余额，之后为承前页余额（= 本页第一行余额 − 增加 + 减少）
const rows = computed(() => {
  const first = lines.value[0]
  if (pagination.current === 1 || !first) {
    return [{ id: OPENING, type: OPENING, bizDate: props.statement.beginDate, balance: props.statement.opening }, ...lines.value]
  }
  const carried = Number(first.balance) - Number(first.increase || 0) + Number(first.decrease || 0)
  return [{ id: CARRIED, type: CARRIED, bizDate: '', balance: carried }, ...lines.value]
})

let seq = 0
async function load() {
  const current = ++seq
  loading.value = true
  try {
    const res = await props.linesApi(props.statement.id, { current: pagination.current, size: pagination.pageSize })
    if (current !== seq) return
    lines.value = res.records
    pagination.total = res.total
  } finally {
    if (current === seq) loading.value = false
  }
}
function onTableChange(p) {
  pagination.current = p.current
  pagination.pageSize = p.pageSize
  load()
}
watch(() => props.statement.id, () => {
  pagination.current = 1
  load()
}, { immediate: true })

const title = () => t('fin.partnerStatement.customerTitle')
const periodText = () => t('fin.partnerStatement.range', { begin: props.statement.beginDate, end: props.statement.endDate })
const allRows = async () => withOpening((await props.linesApi(props.statement.id, {})).records, props.statement.beginDate, props.statement.opening)

const printing = ref(false)
async function print() {
  printing.value = true
  try {
    const s = props.statement
    const fields = [
      printField(labels.partner, `${s.customerCode || ''} ${s.customerName || ''}`.trim(), 'customer'),
      printField(t('fin.partnerStatement.statementPeriod'), periodText(), 'statementPeriod'),
      printField(t('fin.partnerStatement.opening'), formatMoney(s.opening), 'beginBalance'),
      printField(t('fin.customerStatement.billAmount'), formatMoney(s.billAmount), 'periodAr'),
      printField(t('fin.customerStatement.receiptAmount'), formatMoney(s.receiptAmount), 'periodReceipt'),
      printField(t('fin.customerStatement.discountAmount'), formatMoney(s.discountAmount), 'periodDiscount'),
      printField(t('fin.customerStatement.nettingAmount'), formatMoney(s.nettingAmount), 'periodNetting'),
      printField(t('fin.customerStatement.closing'), formatMoney(s.closing), 'endBalance')
    ]
    await printDocs(ledgerPrintDoc({ title: title(), no: s.statementNo, fields, rows: await allRows(), totals: totals.value, labels, dict }),
      `${title()} ${s.statementNo}`, 'fin.customerStatement')
  } finally {
    printing.value = false
  }
}
const exporting = ref(false)
async function exportAll() {
  exporting.value = true
  try {
    const s = props.statement
    const header = [
      [t('fin.customerStatement.statementNo'), s.statementNo],
      ...(props.portal ? [] : [[labels.partner, `${s.customerCode || ''} ${s.customerName || ''}`.trim()]]),
      [t('fin.partnerStatement.statementPeriod'), periodText()]
    ]
    await exportLedger(`${title()}_${s.statementNo}`, header, await allRows(), totals.value, labels, dict)
  } finally {
    exporting.value = false
  }
}

defineExpose({ reload: load })
</script>

<style scoped>
.block {
  margin-bottom: 16px;
}
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}
.section-title {
  font-weight: 600;
}
.attachments {
  margin: 16px 0 8px;
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
