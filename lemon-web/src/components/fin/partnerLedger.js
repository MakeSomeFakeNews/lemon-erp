/**
 * 往来明细的显示、打印、导出（往来对账单页面与客户对账单共用，issue #134）：
 * 应收应付单计入增加，收付款（含折让）、预收预付、对冲计入减少，逐笔余额
 */
import { t } from '@/locales'
import zhPrint from '@/locales/zh-CN/print'
import { BillKind, PartnerLedgerType } from '@/constants/enums'
import { findDict } from '@/utils/dict'
import { writeExcel } from '@/utils/excel'
import { formatMoney, moneyOrBlank } from './fin'

/** 前端合成的期初行、承前页行（不是后端 PartnerLedgerType 的取值） */
export const OPENING = 'OPENING'
export const CARRIED = 'CARRIED'

/** 打印名称（中文匹配键） */
const pn = zhPrint.name

/** 按往来类型（客户 / 供应商）区分的文字 */
export function ledgerLabels(kind) {
  return kind === BillKind.AP
    ? {
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
    : {
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
}

export function ledgerTypeText(r, labels) {
  if (r.linkedAp) {
    return {
      BILL: t('fin.linkedAp.apBill'),
      SETTLEMENT: t('fin.linkedAp.payment'),
      ADVANCE: t('fin.linkedAp.prepayment'),
      NETTING: t('fin.linkedAp.netting')
    }[r.type]
  }
  return {
    OPENING: t('fin.partnerStatement.opening'),
    CARRIED: t('fin.partnerStatement.carried'),
    BILL: labels.bill,
    SETTLEMENT: labels.settle,
    ADVANCE: labels.advance,
    NETTING: t('fin.partnerStatement.netting')
  }[r.type]
}

/** 来源（应收应付单）/ 结算方式（收付款、预收预付）；dict 需要 fin_bill_source、fin_settle_method */
export function ledgerSourceText(r, dict) {
  if (r.type === OPENING || r.type === CARRIED || r.type === PartnerLedgerType.NETTING) return ''
  if (r.type === PartnerLedgerType.BILL) {
    const src = findDict(dict.fin_bill_source, r.sourceType)?.label || r.sourceType || ''
    return [src, r.sourceNo].filter(Boolean).join(' ')
  }
  return findDict(dict.fin_settle_method, r.sourceType)?.label || r.sourceType || ''
}

/** 减少金额中含折让时注明 */
export function decreaseText(r) {
  const v = moneyOrBlank(r.decrease)
  return Number(r.discount) ? `${v} (${t('fin.partnerStatement.discountOf', { amount: formatMoney(r.discount) })})` : v
}

/** 表格列（bodyCell 中 key 为 type / source / increase / decrease / balance） */
export function ledgerColumns(labels) {
  return [
    { title: t('common.field.date'), dataIndex: 'bizDate', width: 110 },
    { title: t('common.field.type'), key: 'type', width: 130 },
    { title: t('fin.partnerStatement.docNo'), dataIndex: 'docNo', width: 160 },
    { title: t('fin.partnerStatement.source'), key: 'source', width: 200, ellipsis: true },
    { title: labels.billAmount, key: 'increase', width: 150, align: 'right' },
    { title: labels.settleAmount, key: 'decrease', width: 170, align: 'right' },
    { title: t('fin.partnerStatement.balance'), key: 'balance', width: 140, align: 'right' },
    { title: t('common.field.remark'), dataIndex: 'remark', ellipsis: true }
  ]
}

/** 期初行 + 明细（打印、导出） */
export function withOpening(rows, bizDate, opening) {
  return [{ type: OPENING, bizDate, balance: opening }, ...rows]
}

/**
 * 往来明细的打印单据（printDocs 的 doc）
 *
 * @param {object} p
 * @param {string} p.title 单据名称
 * @param {string} [p.no] 单号
 * @param {Array} p.fields 表头字段（printDocs 的 fields）
 * @param {Array} p.rows 含期初行的全部明细
 * @param {object} p.totals { increase, decrease, closing }
 * @param {object} p.labels ledgerLabels 的结果
 * @param {object} p.dict 字典（fin_bill_source、fin_settle_method）
 */
export function ledgerPrintDoc({ title, no, fields, rows, totals, labels, dict }) {
  return {
    title,
    no,
    fields,
    columns: [
      { title: t('common.field.date'), key: 'bizDate' },
      { title: t('common.field.type'), key: 'type', value: (r) => ledgerTypeText(r, labels) },
      { title: t('fin.partnerStatement.docNo'), key: 'docNo' },
      { title: t('fin.partnerStatement.source'), key: 'source', value: (r) => ledgerSourceText(r, dict) },
      { title: t('fin.partnerStatement.increase'), key: 'increase', value: (r) => moneyOrBlank(r.increase), align: 'right' },
      { title: t('fin.partnerStatement.decrease'), key: 'decrease', value: decreaseText, align: 'right' },
      { title: t('fin.partnerStatement.balance'), key: 'balance', value: (r) => formatMoney(r.balance), align: 'right' },
      { title: t('common.field.remark'), key: 'remark' }
    ],
    rows,
    totals: {
      bizDate: t('fin.partnerStatement.periodTotal'),
      increase: formatMoney(totals.increase),
      decrease: formatMoney(totals.decrease),
      balance: formatMoney(totals.closing)
    },
    signs: [t('fin.partnerStatement.preparer'), t('fin.partnerStatement.financeReview'), labels.confirm],
    note: t('fin.partnerStatement.note')
  }
}

/** 表头字段 [显示名称, 值, 整行, 中文匹配键]：匹配键见 locales/zh-CN/print.js 的 name */
export function printField(label, value, nameKey) {
  return [label, value, false, pn[nameKey]]
}

/**
 * 导出往来明细（含期初行和合计行）
 *
 * @param {string} name 文件名
 * @param {Array<[string, any]>} header 表头信息（导出在明细之前的几行）
 */
export function exportLedger(name, header, rows, totals, labels, dict) {
  const num = (v) => (v === null || v === undefined || v === '' ? '' : Number(v))
  const headers = [
    { title: t('common.field.date'), width: 12 },
    { title: t('common.field.type'), width: 14 },
    { title: t('fin.partnerStatement.docNo'), width: 20 },
    { title: t('fin.partnerStatement.source'), width: 24 },
    { title: labels.billAmount, width: 14 },
    { title: labels.settleAmount, width: 14 },
    { title: t('fin.partnerStatement.discount'), width: 12 },
    { title: t('fin.partnerStatement.balance'), width: 14 },
    { title: t('common.field.remark'), width: 30 }
  ]
  const data = [
    ...header.map(([k, v]) => [k, v]),
    ...rows.map((r) => [r.bizDate, ledgerTypeText(r, labels), r.docNo || '', ledgerSourceText(r, dict), num(r.increase), num(r.decrease),
      num(r.discount), num(r.balance), r.remark || '']),
    [t('fin.partnerStatement.periodTotal'), '', '', '', num(totals.increase), num(totals.decrease), '', num(totals.closing), '']
  ]
  return writeExcel(name, headers, data)
}
