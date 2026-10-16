import dayjs from 'dayjs'

/** 项目状态操作：当前状态 → 可执行的操作（与后端 PmProjectService 的状态流转一致） */
export const STATUS_ACTIONS = {
  PLANNING: [{ status: 'ACTIVE', label: '启动', confirm: '启动后开始记录实际开始日期，成员可以更新进度、填报工时' }],
  ACTIVE: [
    { status: 'SUSPENDED', label: '暂停', confirm: '暂停后不能更新任务进度和领料' },
    { status: 'FINISHED', label: '完成', confirm: '完成后不能再填报工时、录入费用和领料' }
  ],
  SUSPENDED: [{ status: 'ACTIVE', label: '恢复', confirm: '恢复项目为进行中' }],
  FINISHED: [{ status: 'CLOSED', label: '关闭', confirm: '关闭后项目不能再修改' }],
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
