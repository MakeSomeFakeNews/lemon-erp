/** 生产看板与生产进度大屏共用：状态颜色、日期距离 */
import dayjs from 'dayjs'
import { t } from '@/locales'

/** 工单状态：RUNNING 今天有报工 / IDLE 停滞 / WAITING 待开工 / TO_STOCK 待入库 */
export const ORDER_STATE_COLORS = { RUNNING: '#1677ff', IDLE: '#fa8c16', WAITING: '#8c8c8c', TO_STOCK: '#52c41a' }
/** 交期风险 */
export const RISK_COLORS = { ON_TIME: '#52c41a', AT_RISK: '#fa8c16', OVERDUE: '#ff4d4f', UNKNOWN: '#8c8c8c' }
/** 产线状态 */
export const LINE_STATE_COLORS = { RUNNING: '#52c41a', IDLE: '#fa8c16', REST: '#8c8c8c', FREE: '#1677ff' }
/** 工序状态 */
export const OP_STATE_COLORS = { DONE: '#52c41a', DOING: '#1677ff', WAITING: 'rgba(128, 128, 128, 0.35)' }

export const orderStateLabel = (s) => t(`mes.board.orderState.${s}`)
export const riskLabel = (s) => t(`mes.board.risk.${s}`)
export const lineStateLabel = (s) => t(`mes.board.lineState.${s}`)
export const opStateLabel = (s) => t(`mes.board.opState.${s}`)

/** 距今天的天数：今天 0，明天 1 */
export const daysFromToday = (d) => (d ? dayjs(d).startOf('day').diff(dayjs().startOf('day'), 'day') : null)

/** 预计完工的说明：今天 / 明天 / N 天后 / 无法估算 */
export function etaText(d) {
  const n = daysFromToday(d)
  if (n === null) return t('mes.board.etaUnknown')
  if (n <= 0) return t('mes.board.etaToday')
  if (n === 1) return t('mes.board.etaTomorrow')
  return t('mes.board.etaDays', { n })
}

/** 工序的提示：10 冲压 · 冲压线 · 合格 80 / 100 · 预计 10-19 */
export function opTip(op, qty) {
  const parts = [`${op.seq} ${op.name}`]
  if (op.workCenterName) parts.push(op.workCenterName)
  parts.push(opStateLabel(op.state))
  parts.push(t('mes.board.opDone', { good: Number(op.goodQty), qty: Number(qty) }))
  if (Number(op.scrapQty)) parts.push(t('mes.board.opScrap', { n: Number(op.scrapQty) }))
  if (Number(op.todayGood)) parts.push(t('mes.board.opToday', { n: Number(op.todayGood) }))
  if (op.state !== 'DONE') parts.push(op.eta ? t('mes.board.opEta', { d: String(op.eta).slice(5) }) : t('mes.board.etaUnknown'))
  return parts.join(' · ')
}
