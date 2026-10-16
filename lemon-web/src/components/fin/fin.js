import dayjs from 'dayjs'

export { formatMoney } from '@/components/scm/scm'

/** dayjs / 'YYYY-MM' → 会计期间 yyyyMM */
export const toPeriod = (d) => Number(dayjs(d).format('YYYYMM'))

/** 会计期间 yyyyMM → 'YYYY-MM'（月份选择器的值） */
export const periodValue = (p) => (p ? `${String(p).slice(0, 4)}-${String(p).slice(4)}` : undefined)

/** 会计期间显示：2026 年 10 月 */
export const periodText = (p) => (p ? `${String(p).slice(0, 4)} 年 ${Number(String(p).slice(4))} 月` : '-')

export const currentPeriod = () => toPeriod(dayjs())

/** 金额为 0 时显示为空，账簿中更易读 */
export function moneyOrBlank(n) {
  if (n === null || n === undefined || Number(n) === 0) return ''
  return Number(n).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

export { cached } from '@/utils/cache'

/**
 * 辅助核算项目（与后端 AuxItem 一致）：key 为分录上的 ID 字段，name 为分录上的名称字段。
 * 科目上设置的项目在手工凭证中必填，各项目可同时填写
 */
export const AUX_ITEMS = [
  { value: 'DEPT', label: '部门', key: 'deptId', name: 'deptName' },
  { value: 'CUSTOMER', label: '客户', key: 'customerId', name: 'customerName' },
  { value: 'SUPPLIER', label: '供应商', key: 'supplierId', name: 'supplierName' },
  { value: 'PROJECT', label: '项目', key: 'projectId', name: 'projectName' },
  { value: 'MATERIAL', label: '存货', key: 'materialId', name: 'materialName' }
]

/** 科目上保存的辅助核算（逗号分隔）→ 数组 */
export const auxItemsOf = (account) => (account?.auxItems ? account.auxItems.split(',') : [])

/** 分录的辅助核算显示文本：部门 / 客户 / … */
export const auxText = (entry) => AUX_ITEMS.map((a) => entry[a.name]).filter(Boolean).join(' / ')

/** 金额大写，如 1234.5 → 壹仟贰佰叁拾肆元伍角整 */
export function moneyUpper(n) {
  const num = Math.round(Math.abs(Number(n) || 0) * 100)
  if (num === 0) return '零元整'
  const digits = '零壹贰叁肆伍陆柒捌玖'
  const units = ['', '拾', '佰', '仟']
  const groups = ['', '万', '亿', '万亿']
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
        if (zero) text += '零'
        zero = false
        text += digits[d] + units[pos % 4]
      }
      if (pos % 4 === 0 && pos > 0) {
        if (!text.endsWith('亿') && !text.endsWith('万')) text += groups[pos / 4]
        zero = false
      }
    }
    text += '元'
  }
  if (jiao === 0 && fen === 0) return (Number(n) < 0 ? '负' : '') + text + '整'
  if (jiao > 0) text += digits[jiao] + '角'
  else if (yuan > 0) text += '零'
  if (fen > 0) text += digits[fen] + '分'
  return (Number(n) < 0 ? '负' : '') + text
}

/** 与业务单据金额联动的凭证来源，不能直接删除（在业务单据中撤销或做红字冲销） */
export const UNDELETABLE_SOURCES = ['ADVANCE', 'BAD_DEBT', 'CLAIM', 'LOAN', 'NOTE', 'TRANSFER']

/** 可以红字冲销：已记账、未冲销、本身不是红字凭证、不是结转损益凭证 */
export const canReverse = (v) => v.status === 'POSTED' && v.reversed !== 1 && !v.reversalOf && v.sourceType !== 'CLOSE'

/** 可以删除：未审核，且不是与业务单据联动的凭证 */
export const canDeleteVoucher = (v) => v.status === 'DRAFT' && !UNDELETABLE_SOURCES.includes(v.sourceType)
