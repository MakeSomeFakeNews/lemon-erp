import { t } from '@/locales'
import { today } from '@/utils/format'

/** 员工自助的公共函数 */

/** yyyy-MM-dd HH:mm:ss → HH:mm；为空时 --:-- */
export const timeText = (v) => (v ? v.slice(11, 16) : '--:--')

/**
 * 今天已打上班卡、还没打下班卡：库里的状态是缺卡（下班打卡或补录后重新计算），界面上显示为「未打下班卡」，
 * 与月度汇总一致，当天不计缺卡
 */
export const awaitingCheckOut = (a) => !!a && a.workDate === today() && !!a.checkIn && !a.checkOut

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
