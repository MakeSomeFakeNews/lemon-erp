import dayjs from 'dayjs'
import { t } from '@/locales'

export const GENDERS = [
  { value: 'M', label: t('hr.common.male') },
  { value: 'F', label: t('hr.common.female') }
]
export const genderText = (g) => (g === 'M' ? t('hr.common.male') : g === 'F' ? t('hr.common.female') : '-')

/** 人事异动类型（转正只对试用期员工） */
export const CHANGE_TYPES = [
  { value: 'REGULAR', label: t('hr.common.change.REGULAR') },
  { value: 'TRANSFER', label: t('hr.common.change.TRANSFER') },
  { value: 'SALARY', label: t('hr.common.change.SALARY') },
  { value: 'LEAVE', label: t('hr.common.change.LEAVE') }
]

export const INTERVIEW_METHODS = [
  { value: 'ONSITE', label: t('hr.common.interview.ONSITE') },
  { value: 'VIDEO', label: t('hr.common.interview.VIDEO') },
  { value: 'PHONE', label: t('hr.common.interview.PHONE') }
]

export const PERIOD_TYPES = [
  { value: 'MONTH', label: t('hr.common.periodType.MONTH') },
  { value: 'QUARTER', label: t('hr.common.periodType.QUARTER') },
  { value: 'YEAR', label: t('hr.common.periodType.YEAR') }
]

/** 小时 → 天（按 8 小时） */
export const hoursText = (h) => {
  const n = Number(h) || 0
  return n >= 8 ? t('hr.common.daysHours', { days: +(n / 8).toFixed(2), hours: n }) : t('hr.common.hours', { hours: n })
}

export const timeText = (t) => (t ? dayjs(t).format('HH:mm') : '-')

/** yyyyMM ↔ dayjs */
export const periodOf = (d) => Number(dayjs(d).format('YYYYMM'))
export const periodDate = (p) => (p ? dayjs(`${String(p).slice(0, 4)}-${String(p).slice(4)}-01`) : undefined)
export const periodText = (p) => (p ? t('hr.common.periodText', { year: String(p).slice(0, 4), month: Number(String(p).slice(4)) }) : '-')

/** 专项附加扣除的标准扣除额（元/月）：选择类型时作为默认金额 */
export const DEDUCTION_STANDARD = {
  CHILD_EDU: { amount: 2000, hint: t('hr.common.deductionHint.CHILD_EDU') },
  CONTINUING_EDU: { amount: 400, hint: t('hr.common.deductionHint.CONTINUING_EDU') },
  HOUSING_LOAN: { amount: 1000, hint: t('hr.common.deductionHint.HOUSING_LOAN') },
  HOUSING_RENT: { amount: 1500, hint: t('hr.common.deductionHint.HOUSING_RENT') },
  ELDERLY: { amount: 3000, hint: t('hr.common.deductionHint.ELDERLY') },
  INFANT: { amount: 2000, hint: t('hr.common.deductionHint.INFANT') }
}
