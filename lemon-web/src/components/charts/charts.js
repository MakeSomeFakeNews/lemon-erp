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

/** 保留至多 digits 位小数并去掉末尾的 0（同时消除浮点误差，0.30000000000000004 → 0.3） */
const trimDigits = (v, digits) => String(Number(v.toFixed(digits)))

/**
 * 数值轴刻度：按万 / 亿（英文 K / M / B）显示，去掉多余的 0（400.0 万 → 400 万）。
 * 刻度本身是整齐的数，小数位按刻度值保留（最多 6 位），不固定为 1~2 位：
 * 否则数据范围很小时（如 SPC 控制图 1.600 ~ 1.615）相邻刻度舍入成同一个值
 */
export function axisNumber(n) {
  const v = Number(n) || 0
  const a = Math.abs(v)
  if (isZh()) {
    if (a >= 1e8) return t('components.chart.yi', { n: trimDigits(v / 1e8, 6) })
    if (a >= 1e4) return t('components.chart.wan', { n: trimDigits(v / 1e4, 6) })
  } else {
    if (a >= 1e9) return `${trimDigits(v / 1e9, 6)} B`
    if (a >= 1e6) return `${trimDigits(v / 1e6, 6)} M`
    if (a >= 1e4) return `${trimDigits(v / 1e3, 6)} K`
  }
  return Number(v.toFixed(6)).toLocaleString('zh-CN', { maximumFractionDigits: 6 })
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
