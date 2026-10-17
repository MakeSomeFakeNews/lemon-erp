import dayjs from 'dayjs'
import { t } from '@/locales'

/** 项目状态操作：当前状态 → 可执行的操作（与后端 PmProjectService 的状态流转一致） */
export const STATUS_ACTIONS = {
  PLANNING: [{ status: 'ACTIVE', label: t('pm.status.start'), confirm: t('pm.status.startTip') }],
  ACTIVE: [
    { status: 'SUSPENDED', label: t('pm.status.suspend'), confirm: t('pm.status.suspendTip') },
    { status: 'FINISHED', label: t('pm.status.finish'), confirm: t('pm.status.finishTip') }
  ],
  SUSPENDED: [{ status: 'ACTIVE', label: t('pm.status.resume'), confirm: t('pm.status.resumeTip') }],
  FINISHED: [{ status: 'CLOSED', label: t('pm.status.close'), confirm: t('pm.status.closeTip') }],
  CLOSED: []
}

/** 成本类型（与后端 PmCostService.COST_TYPES 一致），人工成本来自工时 */
export const COST_TYPES = ['LABOR', 'MATERIAL', 'TRAVEL', 'OUTSOURCE', 'EQUIPMENT', 'OTHER']
export const MANUAL_COST_TYPES = COST_TYPES.filter((t) => t !== 'LABOR')

/** 绩效指数标签：≥ 1 绿色，< 1 红色 */
export function indexTag(v) {
  if (v === null || v === undefined) return { text: '-', color: undefined }
  return { text: Number(v).toFixed(2), color: Number(v) >= 1 ? 'green' : 'red' }
}

export const num = (v) => Number(v) || 0

/** 实际成本 = 人工 + 费用 */
export const actualCost = (p) => num(p.laborCost) + num(p.expenseCost)

/** 任务树拍平，附带层级 level */
export function flattenTasks(tree, level = 0, out = []) {
  for (const t of tree || []) {
    out.push({ ...t, level })
    if (t.children?.length) flattenTasks(t.children, level + 1, out)
  }
  return out
}

/** 末级任务 */
export const leafTasks = (tree) => flattenTasks(tree).filter((t) => !t.children?.length)

/** 本周一、周日 */
export function weekRange(d = dayjs()) {
  const day = dayjs(d)
  const monday = day.subtract((day.day() + 6) % 7, 'day')
  return [monday.format('YYYY-MM-DD'), monday.add(6, 'day').format('YYYY-MM-DD')]
}

export const today = () => dayjs().format('YYYY-MM-DD')
