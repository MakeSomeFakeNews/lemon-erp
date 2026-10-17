/**
 * 数字格式化与舍入（与后端 Decimals 一致：金额 2 位、数量和单价 4 位，四舍五入）。
 * 页面中不要再各自写 toLocaleString / Math.round，统一从这里取。
 */

/** 显示用的区域：千分位、小数点固定按中文格式（与历史显示一致） */
const LOCALE = 'zh-CN'

const isBlank = (n) => n === null || n === undefined || n === ''

/**
 * 千分位格式化
 * @param {number|string} n
 * @param {object}  [opts]
 * @param {number}  [opts.min=0]    最少小数位
 * @param {number}  [opts.max=opts.min] 最多小数位
 * @param {string}  [opts.locale]   区域，默认 zh-CN
 */
export function formatDecimal(n, { min = 0, max = min, locale = LOCALE } = {}) {
  return Number(n).toLocaleString(locale, { minimumFractionDigits: min, maximumFractionDigits: max })
}

/** 金额：千分位、2 位小数；空值显示 empty（默认 -） */
export function money(n, empty = '-') {
  if (isBlank(n)) return empty
  return formatDecimal(n, { min: 2, max: 2 })
}

/** 金额为 0 或空时显示为空，账簿中更易读 */
export function moneyOrBlank(n) {
  if (n === null || n === undefined || Number(n) === 0) return ''
  return formatDecimal(n, { min: 2, max: 2 })
}

/** 单价：千分位，2～4 位小数 */
export const price = (n) => formatDecimal(n, { min: 2, max: 4 })

/** 数量：千分位，去掉多余的 0，最多 digits 位小数；空值显示 - */
export function qty(n, digits = 4, locale = LOCALE) {
  if (isBlank(n)) return '-'
  return formatDecimal(n, { max: digits, locale })
}

/** 数量：千分位，最多 digits 位小数（默认整数）；空值或非数字显示 - */
export function num(v, digits = 0) {
  if (isBlank(v)) return '-'
  const n = Number(v)
  if (Number.isNaN(n)) return '-'
  return formatDecimal(n, { max: digits })
}

/** 四舍五入到分（金额） */
export const round2 = (n) => Math.round(n * 100) / 100

/** 四舍五入到 4 位（数量），避免浮点误差（如 0.3 - 0.1）提交到后端超出精度 */
export const round4 = (n) => Math.round(n * 10000) / 10000

/** 空值、非数字按 0 处理 */
export const nz = (n) => Number(n) || 0

/** 对列表中某个字段（或函数取值）求和，结果按 round 舍入（默认 round2） */
export function sumBy(list, field, round = round2) {
  const get = typeof field === 'function' ? field : (r) => r[field]
  return round((list || []).reduce((s, r) => s + nz(get(r)), 0))
}
