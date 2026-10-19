/** 设备与质量分包的公共逻辑（与 Web 端 components/qms/qms.js 的检验规则一致） */
import { today } from '@/utils/format'

/** 下次保养日期的提示：已到期 / 7 天内到期，其余为空 */
export function dueText(date, t) {
  if (!date) return ''
  const days = Math.round((new Date(date.replace(/-/g, '/')) - new Date(today().replace(/-/g, '/'))) / 86400000)
  if (days < 0) return t('mes.equipment.overdue')
  if (days <= 7) return t('mes.equipment.dueSoon', { n: days })
  return ''
}

/** 实测值文本（逗号、空格、换行分隔）解析为数字 */
export function parseMeasured(text) {
  return String(text ?? '')
    .split(/[\s,，;；]+/) // i18n-ignore 全角分隔符
    .filter((s) => s !== '')
    .map(Number)
    .filter((n) => !Number.isNaN(n))
}

export const outOfSpec = (v, item) =>
  (item.lowerLimit != null && v < Number(item.lowerLimit)) || (item.upperLimit != null && v > Number(item.upperLimit))

/** 规格：下限 ~ 上限 单位；没有上下限时为标准说明 */
export function specText(item) {
  const lo = item.lowerLimit != null ? Number(item.lowerLimit) : null
  const hi = item.upperLimit != null ? Number(item.upperLimit) : null
  const unit = item.unit ? ` ${item.unit}` : ''
  if (lo != null && hi != null) return `${lo} ~ ${hi}${unit}`
  if (lo != null) return `≥ ${lo}${unit}`
  if (hi != null) return `≤ ${hi}${unit}`
  return item.standard || '-'
}

/** 工单号查工单（精确匹配） */
export async function findWorkOrder(pageWorkOrder, woNo) {
  const page = await pageWorkOrder({ woNo, size: 5 })
  return (page?.records || []).find((w) => w.woNo === woNo) || null
}
