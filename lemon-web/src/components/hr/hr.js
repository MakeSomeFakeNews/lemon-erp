import dayjs from 'dayjs'

export const GENDERS = [
  { value: 'M', label: '男' },
  { value: 'F', label: '女' }
]
export const genderText = (g) => (g === 'M' ? '男' : g === 'F' ? '女' : '-')

/** 人事异动类型（转正只对试用期员工） */
export const CHANGE_TYPES = [
  { value: 'REGULAR', label: '转正' },
  { value: 'TRANSFER', label: '调岗' },
  { value: 'SALARY', label: '调薪' },
  { value: 'LEAVE', label: '离职' }
]

export const INTERVIEW_METHODS = [
  { value: 'ONSITE', label: '现场' },
  { value: 'VIDEO', label: '视频' },
  { value: 'PHONE', label: '电话' }
]

export const PERIOD_TYPES = [
  { value: 'MONTH', label: '月度' },
  { value: 'QUARTER', label: '季度' },
  { value: 'YEAR', label: '年度' }
]

/** 小时 → 天（按 8 小时） */
export const hoursText = (h) => {
  const n = Number(h) || 0
  return n >= 8 ? `${+(n / 8).toFixed(2)} 天（${n} 小时）` : `${n} 小时`
}

export const timeText = (t) => (t ? dayjs(t).format('HH:mm') : '-')

/** yyyyMM ↔ dayjs */
export const periodOf = (d) => Number(dayjs(d).format('YYYYMM'))
export const periodDate = (p) => (p ? dayjs(`${String(p).slice(0, 4)}-${String(p).slice(4)}-01`) : undefined)
export const periodText = (p) => (p ? `${String(p).slice(0, 4)} 年 ${Number(String(p).slice(4))} 月` : '-')

/** 专项附加扣除的标准扣除额（元/月）：选择类型时作为默认金额 */
export const DEDUCTION_STANDARD = {
  CHILD_EDU: { amount: 2000, hint: '每个子女每月 2000 元，父母可选择一方全额扣除或双方各扣 50%' },
  CONTINUING_EDU: { amount: 400, hint: '学历（学位）继续教育每月 400 元，最长 48 个月；职业资格证书在取得当年按 3600 元一次扣除' },
  HOUSING_LOAN: { amount: 1000, hint: '首套住房贷款利息每月 1000 元，最长 240 个月' },
  HOUSING_RENT: { amount: 1500, hint: '直辖市、省会等城市 1500 元；市辖区户籍人口超过 100 万的城市 1100 元，其余 800 元' },
  ELDERLY: { amount: 3000, hint: '独生子女每月 3000 元；非独生子女与兄弟姐妹分摊，每人不超过 1500 元' },
  INFANT: { amount: 2000, hint: '3 岁以下婴幼儿每个每月 2000 元' }
}
