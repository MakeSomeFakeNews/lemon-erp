/** 质量管理的常量与工具 */

/** GB/T 2828.1 检验水平：特殊检验水平 S-1 ~ S-4（小样本、破坏性检验），一般检验水平 I ~ III（通常用 II） */
export const INSPECTION_LEVELS = [
  { value: 'S1', label: 'S-1' },
  { value: 'S2', label: 'S-2' },
  { value: 'S3', label: 'S-3' },
  { value: 'S4', label: 'S-4' },
  { value: 'I', label: 'I' },
  { value: 'II', label: 'II（一般）' },
  { value: 'III', label: 'III' }
]

/** GB/T 2828.1 表 2-A 可选的 AQL */
export const AQL_OPTIONS = ['0.040', '0.065', '0.10', '0.15', '0.25', '0.40', '0.65', '1.0', '1.5', '2.5', '4.0', '6.5']
  .map((v) => ({ value: Number(v), label: v }))

export const levelLabel = (v) => INSPECTION_LEVELS.find((l) => l.value === v)?.label || v

/** 方案的抽样说明 */
export function samplingText(plan) {
  if (!plan) return ''
  if (plan.samplingMode === 'FULL') return '全检'
  if (plan.samplingMode === 'FIXED') return `固定抽 ${Number(plan.fixedQty)} 件，允收 ${plan.acceptQty ?? 0}`
  return `AQL ${Number(plan.aql)} · 检验水平 ${levelLabel(plan.inspectionLevel)}`
}

/** 实测值文本（逗号、空格、换行分隔）解析为数字 */
export function parseMeasured(text) {
  return String(text ?? '')
    .split(/[\s,，;；]+/)
    .filter((s) => s !== '')
    .map(Number)
    .filter((n) => !Number.isNaN(n))
}

export const outOfSpec = (v, item) =>
  (item.lowerLimit != null && v < Number(item.lowerLimit)) || (item.upperLimit != null && v > Number(item.upperLimit))

/** 规格：下限 ~ 上限 单位 */
export function specText(item) {
  const lo = item.lowerLimit != null ? Number(item.lowerLimit) : null
  const hi = item.upperLimit != null ? Number(item.upperLimit) : null
  const unit = item.unit ? ` ${item.unit}` : ''
  if (lo != null && hi != null) return `${lo} ~ ${hi}${unit}`
  if (lo != null) return `≥ ${lo}${unit}`
  if (hi != null) return `≤ ${hi}${unit}`
  return item.standard || '-'
}
