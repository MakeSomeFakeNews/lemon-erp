import { isZh } from '@/locales'

/** 金额：千分位两位小数 */
export function money(v) {
  if (v === null || v === undefined || v === '') return '-'
  const n = Number(v)
  return Number.isFinite(n) ? n.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : String(v)
}

/** 数量：去掉多余的 0，最多 4 位小数 */
export function qty(v) {
  if (v === null || v === undefined || v === '') return '-'
  const n = Number(v)
  return Number.isFinite(n) ? String(Math.round(n * 10000) / 10000) : String(v)
}

/** 大金额缩写（工作台卡片）：中文按万、亿，英文按 K、M */
export function shortMoney(v) {
  const n = Number(v) || 0
  const abs = Math.abs(n)
  if (isZh()) {
    if (abs >= 1e8) return `${(n / 1e8).toFixed(2)}亿` // i18n-ignore 数量单位
    if (abs >= 1e4) return `${(n / 1e4).toFixed(2)}万` // i18n-ignore 数量单位
  } else {
    if (abs >= 1e6) return `${(n / 1e6).toFixed(2)}M`
    if (abs >= 1e3) return `${(n / 1e3).toFixed(1)}K`
  }
  return money(n)
}

/** 时间显示到分钟：2026-10-18 09:30:00 → 10-18 09:30（今年）/ 2025-10-18 09:30 */
export function shortTime(s) {
  if (!s) return ''
  const year = String(new Date().getFullYear())
  const v = s.slice(0, 16)
  return v.startsWith(year) ? v.slice(5) : v
}

/** yyyy-MM-dd */
export function today(offsetDays = 0) {
  const d = new Date(Date.now() + offsetDays * 86400000)
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

/** 期间 yyyyMM（整数，与后端一致） */
export function period(date = new Date()) {
  return date.getFullYear() * 100 + date.getMonth() + 1
}
