import { computed, ref } from 'vue'
import { t } from '@/locales'
import { round2 } from '@/utils/number'
import { InvoiceKind, InvoiceSource, InvoiceStatus, InvoiceType } from '@/constants/enums'

const kindConf = (k, partner) => ({
  label: t(`fin.invoice.kinds.${k}.label`),
  partner,
  partnerLabel: t(`fin.invoice.kinds.${k}.partner`),
  allPartners: t(`fin.invoice.kinds.${k}.allPartners`),
  registerTitle: t(`fin.invoice.kinds.${k}.register`),
  listTitle: t(`fin.invoice.kinds.${k}.list`),
  billsTitle: t(`fin.invoice.kinds.${k}.billsTitle`),
  noBills: t(`fin.invoice.kinds.${k}.noBills`),
  selectPartnerFirst: t(`fin.invoice.kinds.${k}.selectPartnerFirst`),
  autoMatchTip: t(`fin.invoice.kinds.${k}.autoMatchTip`)
})

/** 销项 / 进项发票 */
export const KINDS = {
  [InvoiceKind.OUTPUT]: kindConf(InvoiceKind.OUTPUT, 'customer'),
  [InvoiceKind.INPUT]: kindConf(InvoiceKind.INPUT, 'supplier')
}

export const STATUS_OPTIONS = [
  { value: InvoiceStatus.VALID, label: t('fin.invoice.statusValid') },
  { value: InvoiceStatus.VOID, label: t('fin.invoice.statusVoid') }
]

/** 可认证抵扣的专票（含数电票专票） */
export const SPECIAL_TYPES = [InvoiceType.SPECIAL, InvoiceType.E_SPECIAL]

export const SOURCES = {
  [InvoiceSource.MANUAL]: t('fin.invoice.sources.MANUAL'),
  [InvoiceSource.XML]: t('fin.invoice.sources.XML'),
  [InvoiceSource.EXCEL]: t('fin.invoice.sources.EXCEL')
}

/** 发票未勾稽金额 */
export const unmatchedAmount = (r) => round2(Number(r.amount) - Number(r.matchedAmount || 0))

/** 单据未开票金额 */
export const billRemaining = (b) => round2(Number(b.amount) - Number(b.invoicedAmount || 0))

/** 待勾稽单据列 */
export const billColumns = () => [
  { title: t('fin.invoice.billNo'), dataIndex: 'billNo', width: 150 },
  { title: t('fin.invoice.sourceNo'), dataIndex: 'sourceNo', width: 160, ellipsis: true },
  { title: t('common.field.date'), dataIndex: 'billDate', width: 100 },
  { title: t('common.field.amount'), key: 'amount', width: 120, align: 'right' },
  { title: t('fin.invoice.matched'), key: 'invoiced', width: 110, align: 'right' },
  { title: t('fin.invoice.unmatched'), key: 'remaining', width: 110, align: 'right' },
  { title: t('fin.invoice.applyNow'), key: 'apply', width: 150 }
]

/** 登记、勾稽时勾选单据并填写本次勾稽金额 */
export function useInvoiceBills() {
  const bills = ref([])
  const billLoading = ref(false)
  const checkedBills = ref([])
  const selectedBills = computed(() => bills.value.filter((b) => checkedBills.value.includes(b.id)))
  const itemTotal = computed(() => round2(selectedBills.value.reduce((s, b) => s + (Number(b.apply) || 0), 0)))

  function resetBills() {
    bills.value = []
    checkedBills.value = []
  }

  /** 查询往来单位未全部开票的单据，本次勾稽金额默认为未开票金额 */
  async function fetchBills(loader) {
    billLoading.value = true
    try {
      bills.value = (await loader()).map((b) => ({ ...b, apply: billRemaining(b) }))
    } finally {
      billLoading.value = false
    }
  }

  function onCheckBills(keys) {
    // 新勾选的单据默认勾稽全部未开票金额
    bills.value.forEach((b) => {
      if (keys.includes(b.id) && !checkedBills.value.includes(b.id) && !Number(b.apply)) b.apply = billRemaining(b)
    })
    checkedBills.value = keys
  }

  /** 提交的勾稽明细（金额为 0 的不提交） */
  const billItems = () => selectedBills.value.filter((b) => Number(b.apply)).map((b) => ({ billId: b.id, amount: b.apply }))

  return { bills, billLoading, checkedBills, selectedBills, itemTotal, resetBills, fetchBills, onCheckBills, billItems }
}
