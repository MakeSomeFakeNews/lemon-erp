import { t } from '@/locales'

/** 千分位 */
export const formatNumber = (n) => (n ?? 0).toLocaleString('zh-CN')

/** 尝试格式化 JSON 字符串，失败时原样返回 */
export function prettyJson(text) {
  if (!text) return ''
  try {
    return JSON.stringify(JSON.parse(text), null, 2)
  } catch {
    return text
  }
}

export function greeting(date = new Date()) {
  const h = date.getHours()
  if (h < 6) return t('components.greeting.lateNight')
  if (h < 9) return t('components.greeting.earlyMorning')
  if (h < 12) return t('components.greeting.morning')
  if (h < 14) return t('components.greeting.noon')
  if (h < 18) return t('components.greeting.afternoon')
  return t('components.greeting.evening')
}

/** 字节数转为可读容量，如 1.5 GB */
export function formatBytes(bytes, digits = 1) {
  if (bytes == null || bytes < 0) return '-'
  const units = ['B', 'KB', 'MB', 'GB', 'TB', 'PB']
  let v = bytes
  let i = 0
  while (v >= 1024 && i < units.length - 1) {
    v /= 1024
    i++
  }
  return `${i === 0 ? v : v.toFixed(digits)} ${units[i]}`
}

/** 秒数转为时长，如 3 天 4 小时 5 分；compact 时有天数则省略分钟 */
export function formatDuration(seconds, compact = false) {
  if (seconds == null || seconds < 0) return '-'
  const s = Math.floor(seconds)
  const d = Math.floor(s / 86400)
  const h = Math.floor((s % 86400) / 3600)
  const m = Math.floor((s % 3600) / 60)
  if (d) return compact ? t('components.duration.dh', { d, h }) : t('components.duration.dhm', { d, h, m })
  if (h) return t('components.duration.hm', { h, m })
  if (m) return t('components.duration.ms', { m, s: s % 60 })
  return t('components.duration.s', { s })
}
