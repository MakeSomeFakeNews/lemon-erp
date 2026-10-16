/** 图表常用的数值格式：万元、百分比 */
export function shortMoney(n) {
  const v = Number(n) || 0
  if (Math.abs(v) >= 1e8) return `${(v / 1e8).toFixed(2)} 亿`
  if (Math.abs(v) >= 1e4) return `${(v / 1e4).toFixed(1)} 万`
  return v.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

export const percent = (n, digits = 1) => (n === null || n === undefined ? '-' : `${Number(n).toFixed(digits)}%`)

/** 月份 202610 → 2026-10 / 10月 */
export const periodLabel = (p) => (p ? `${String(p).slice(0, 4)}-${String(p).slice(4)}` : '')
export const monthLabel = (p) => (p ? `${Number(String(p).slice(4))}月` : '')

/** 增长率标签：正数绿色上箭头，负数红色下箭头 */
export function growthTag(v) {
  if (v === null || v === undefined) return { text: '-', color: 'default' }
  const n = Number(v)
  return { text: `${n >= 0 ? '↑' : '↓'} ${Math.abs(n).toFixed(1)}%`, color: n >= 0 ? 'green' : 'red' }
}
