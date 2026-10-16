import dayjs from 'dayjs'

/** 进行中的销售阶段（看板列），与后端 CrmConstants.OPEN_STAGES 一致 */
export const OPEN_STAGES = ['INITIAL', 'NEEDS', 'PROPOSAL', 'NEGOTIATION']
export const ALL_STAGES = [...OPEN_STAGES, 'WON', 'LOST']
/** 各阶段默认赢单概率 */
export const STAGE_PROBABILITY = { INITIAL: 10, NEEDS: 30, PROPOSAL: 50, NEGOTIATION: 80, WON: 100, LOST: 0 }

export const TARGET_TYPES = [
  { value: 'CUSTOMER', label: '客户', color: 'blue' },
  { value: 'LEAD', label: '线索', color: 'orange' },
  { value: 'OPPORTUNITY', label: '商机', color: 'purple' }
]
export const targetType = (v) => TARGET_TYPES.find((t) => t.value === v) || { label: v, color: 'default' }

export const now = () => dayjs().format('YYYY-MM-DD HH:mm:ss')

/** 时间是否已过（下次跟进逾期等） */
export const isPast = (t) => !!t && dayjs(t).isBefore(dayjs())

/** 剩余时间：3 天 2 小时 / 5 小时 20 分；已过返回 null */
export function remaining(t) {
  if (!t) return null
  const minutes = dayjs(t).diff(dayjs(), 'minute')
  if (minutes < 0) return null
  const d = Math.floor(minutes / 1440)
  const h = Math.floor((minutes % 1440) / 60)
  if (d) return `${d} 天 ${h} 小时`
  return `${h} 小时 ${minutes % 60} 分`
}

/** 超时时长 */
export function overdueText(t) {
  const minutes = dayjs().diff(dayjs(t), 'minute')
  const d = Math.floor(minutes / 1440)
  const h = Math.floor((minutes % 1440) / 60)
  return d ? `超时 ${d} 天 ${h} 小时` : `超时 ${h} 小时 ${minutes % 60} 分`
}

/** 金额列表求和 */
export const sum = (list, key) => list.reduce((s, r) => s + (Number(r[key]) || 0), 0)

/** 合同回款计划的回款状态 */
export const COLLECT_STATUS = {
  RECEIVED: { label: '已回款', color: 'green' },
  PARTIAL: { label: '部分回款', color: 'blue' },
  OVERDUE: { label: '逾期', color: 'red' },
  PENDING: { label: '未到期', color: 'default' }
}
