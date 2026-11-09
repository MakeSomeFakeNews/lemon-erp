import dayjs from 'dayjs'
import { t } from '@/locales'
import { round2 } from '@/utils/number'

import { VoucherSource, VoucherStatus } from '@/constants/enums'
export { formatMoney } from '@/components/scm/scm'

/** dayjs / 'YYYY-MM' → 会计期间 yyyyMM */
export const toPeriod = (d) => Number(dayjs(d).format('YYYYMM'))

/** 会计期间 yyyyMM → 'YYYY-MM'（月份选择器的值） */
export const periodValue = (p) => (p ? `${String(p).slice(0, 4)}-${String(p).slice(4)}` : undefined)

/** 会计期间显示：2026 年 10 月 */
export const periodText = (p) =>
  p ? t('fin.finUtil.periodText', { year: String(p).slice(0, 4), month: Number(String(p).slice(4)), mm: String(p).slice(4) }) : '-'

export const currentPeriod = () => toPeriod(dayjs())

/** 金额为 0 时显示为空，账簿中更易读 */
export { moneyOrBlank } from '@/utils/number'

export { cached } from '@/utils/cache'

/**
 * 辅助核算项目（与后端 AuxItem 一致）：key 为分录上的 ID 字段，name 为分录上的名称字段。
 * 科目上设置的项目在手工凭证中必填，各项目可同时填写
 */
export const AUX_ITEMS = [
  { value: 'DEPT', label: t('fin.finUtil.aux.dept'), key: 'deptId', name: 'deptName' },
  { value: 'CUSTOMER', label: t('fin.finUtil.aux.customer'), key: 'customerId', name: 'customerName' },
  { value: 'SUPPLIER', label: t('fin.finUtil.aux.supplier'), key: 'supplierId', name: 'supplierName' },
  { value: 'PROJECT', label: t('fin.finUtil.aux.project'), key: 'projectId', name: 'projectName' },
  { value: 'MATERIAL', label: t('fin.finUtil.aux.material'), key: 'materialId', name: 'materialName' }
]

/** 科目上保存的辅助核算（逗号分隔）→ 数组 */
export const auxItemsOf = (account) => (account?.auxItems ? account.auxItems.split(',') : [])

/** 分录的辅助核算显示文本：部门 / 客户 / … */
export const auxText = (entry) => AUX_ITEMS.map((a) => entry[a.name]).filter(Boolean).join(' / ')

/** 金额大写，如 1234.5 → 壹仟贰佰叁拾肆元伍角整 */
export function moneyUpper(n) {
  const num = Math.round(Math.abs(Number(n) || 0) * 100)
  if (num === 0) return '零元整' // i18n-ignore
  const digits = '零壹贰叁肆伍陆柒捌玖' // i18n-ignore
  const units = ['', '拾', '佰', '仟'] // i18n-ignore
  const groups = ['', '万', '亿', '万亿'] // i18n-ignore
  const yuan = Math.floor(num / 100)
  const jiao = Math.floor(num / 10) % 10
  const fen = num % 10
  let text = ''
  if (yuan > 0) {
    const s = String(yuan)
    let zero = false
    for (let i = 0; i < s.length; i++) {
      const d = Number(s[i])
      const pos = s.length - 1 - i
      if (d === 0) {
        zero = true
      } else {
        if (zero) text += '零' // i18n-ignore
        zero = false
        text += digits[d] + units[pos % 4]
      }
      if (pos % 4 === 0 && pos > 0) {
        if (!text.endsWith('亿') && !text.endsWith('万')) text += groups[pos / 4] // i18n-ignore
        zero = false
      }
    }
    text += '元' // i18n-ignore
  }
  if (jiao === 0 && fen === 0) return (Number(n) < 0 ? '负' : '') + text + '整' // i18n-ignore
  if (jiao > 0) text += digits[jiao] + '角' // i18n-ignore
  else if (yuan > 0) text += '零' // i18n-ignore
  if (fen > 0) text += digits[fen] + '分' // i18n-ignore
  return (Number(n) < 0 ? '负' : '') + text // i18n-ignore
}

/** 与业务单据金额联动的凭证来源，不能直接删除（在业务单据中撤销或做红字冲销） */
export const UNDELETABLE_SOURCES = [VoucherSource.ADVANCE, VoucherSource.BAD_DEBT, VoucherSource.CLAIM, VoucherSource.LOAN, VoucherSource.NOTE, VoucherSource.TRANSFER]

/** 可以红字冲销：已记账、未冲销、本身不是红字凭证、不是结转损益凭证 */
export const canReverse = (v) => v.status === VoucherStatus.POSTED && v.reversed !== 1 && !v.reversalOf && v.sourceType !== VoucherSource.CLOSE

/** 可以删除：未审核，且不是与业务单据联动的凭证 */
export const canDeleteVoucher = (v) => v.status === VoucherStatus.DRAFT && !UNDELETABLE_SOURCES.includes(v.sourceType)

const byBillDate = (a, b) =>
  String(a.billDate || '').localeCompare(String(b.billDate || '')) || String(a.dueDate || '').localeCompare(String(b.dueDate || '')) || a.id - b.id

/**
 * 收付款自动核销：先用红字单据（退货、冲账）全额冲抵，再按单据日期从早到晚（同日按到期日）把目标金额分配到蓝字单据，
 * 早的欠款先结清。收款单 / 付款单、票据收票背书共用。
 *
 * @param {Array}    bills       参与分配的单据（已勾选的）
 * @param {number}   target      本次收付款金额
 * @param {Function} remainingOf (bill) => 可核销余额（已扣除折让，红字为负数）
 * @returns {{ amounts: Map<number, number>, left: number }} 各单据的核销金额，以及分配后剩余（多收 / 多付）的金额
 */
export function allocateSettle(bills, target, remainingOf) {
  const amounts = new Map()
  let left = round2(Number(target) || 0)
  bills.filter((b) => remainingOf(b) < 0).forEach((b) => {
    amounts.set(b.id, remainingOf(b))
    left = round2(left - remainingOf(b))
  })
  for (const b of bills.filter((x) => remainingOf(x) > 0).sort(byBillDate)) {
    if (left <= 0) break
    const v = round2(Math.min(left, remainingOf(b)))
    amounts.set(b.id, v)
    left = round2(left - v)
  }
  return { amounts, left }
}
