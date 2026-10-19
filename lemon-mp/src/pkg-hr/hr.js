import { t } from '@/locales'

/** 员工自助的公共函数 */

/** yyyy-MM-dd HH:mm:ss → HH:mm；为空时 --:-- */
export const timeText = (v) => (v ? v.slice(11, 16) : '--:--')

/** yyyy-MM-dd HH:mm:ss → MM-dd HH:mm */
export const shortDateTime = (v) => (v ? v.slice(5, 16) : '')

/** 期间 yyyyMM 的显示 */
export function periodText(period) {
  const p = Number(period)
  return t('hr.attendance.monthTitle', { year: Math.floor(p / 100), month: p % 100 })
}

/** 期间加减月份 */
export function shiftPeriod(period, delta) {
  const p = Number(period)
  const index = Math.floor(p / 100) * 12 + (p % 100) - 1 + delta
  return Math.floor(index / 12) * 100 + (index % 12) + 1
}

/** 小时数显示：去掉多余的 0 */
export const hoursNum = (v) => Number(v || 0)
