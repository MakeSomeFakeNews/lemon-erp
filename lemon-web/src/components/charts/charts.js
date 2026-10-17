import { isZh, t } from '@/locales'

/** 图表常用的数值格式：万元（英文为 K / M / B）、百分比 */
export function shortMoney(n) {
  const v = Number(n) || 0
  const a = Math.abs(v)
  if (isZh()) {
    if (a >= 1e8) return t('components.chart.yi', { n: (v / 1e8).toFixed(2) })
    if (a >= 1e4) return t('components.chart.wan', { n: (v / 1e4).toFixed(1) })
  } else {
    if (a >= 1e9) return `${(v / 1e9).toFixed(2)} B`
    if (a >= 1e6) return `${(v / 1e6).toFixed(2)} M`
    if (a >= 1e4) return `${(v / 1e3).toFixed(1)} K`
  }
  return v.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

export const percent = (n, digits = 1) => (n === null || n === undefined ? '-' : `${Number(n).toFixed(digits)}%`)

/** 月份 202610 → 2026-10 / 10月 */
export const periodLabel = (p) => (p ? `${String(p).slice(0, 4)}-${String(p).slice(4)}` : '')
export const monthLabel = (p) => (p ? t('components.chart.month', { n: Number(String(p).slice(4)) }) : '')

/** 增长率标签：正数绿色上箭头，负数红色下箭头 */
export function growthTag(v) {
  if (v === null || v === undefined) return { text: '-', color: 'default' }
  const n = Number(v)
  return { text: `${n >= 0 ? '↑' : '↓'} ${Math.abs(n).toFixed(1)}%`, color: n >= 0 ? 'green' : 'red' }
}
