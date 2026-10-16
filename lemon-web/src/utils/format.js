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
  if (h < 6) return '夜深了'
  if (h < 9) return '早上好'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
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

/** 秒数转为时长，如 3 天 4 小时 5 分 */
export function formatDuration(seconds) {
  if (seconds == null || seconds < 0) return '-'
  const s = Math.floor(seconds)
  const d = Math.floor(s / 86400)
  const h = Math.floor((s % 86400) / 3600)
  const m = Math.floor((s % 3600) / 60)
  if (d) return `${d} 天 ${h} 小时 ${m} 分`
  if (h) return `${h} 小时 ${m} 分`
  if (m) return `${m} 分 ${s % 60} 秒`
  return `${s} 秒`
}
