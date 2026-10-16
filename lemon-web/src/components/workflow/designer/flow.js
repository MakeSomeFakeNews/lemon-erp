/**
 * 流程设计器的数据结构与工具函数（与后端 FlowNode / FlowBranch 对应）
 *
 * 节点通过 next 串成链；条件分支 / 并行分支节点的 branches 各自也是一条链（branch.next），
 * 分支汇合后继续执行该节点的 next。开始节点、分支都有 next，因此「插入 / 删除」统一按
 * holder.next 处理：holder 是前一个节点或所在分支。
 */

export const NODE_TYPES = {
  START: 'START',
  APPROVER: 'APPROVER',
  CC: 'CC',
  CONDITION: 'CONDITION',
  PARALLEL: 'PARALLEL'
}

/** 审批人类型；requiresIds：需要选择具体成员 / 角色；single：只会得到一个人，不需要多人审批方式 */
export const ASSIGNEE_TYPES = [
  { label: '指定成员', value: 'USER', requiresIds: true },
  { label: '指定角色', value: 'ROLE', requiresIds: true },
  { label: '部门主管', value: 'DEPT_LEADER', single: true },
  { label: '发起人自己', value: 'INITIATOR', single: true }
]

const assigneeTypeOf = (type) => ASSIGNEE_TYPES.find((t) => t.value === type) || {}
export const requiresIds = (type) => !!assigneeTypeOf(type).requiresIds
export const isSingleAssignee = (type) => !!assigneeTypeOf(type).single

/** 部门主管级别：从发起人所在部门向上数，所在部门为第 1 级 */
export const LEADER_LEVELS = Array.from({ length: 10 }, (_, i) => ({
  label: i === 0 ? '直属主管（发起人所在部门）' : `第 ${i + 1} 级主管`,
  value: i + 1
}))

export const APPROVE_MODES = [
  { label: '依次审批', value: 'SEQUENTIAL', desc: '按顺序依次审批' },
  { label: '会签', value: 'AND', desc: '需所有审批人同意' },
  { label: '或签', value: 'OR', desc: '一名审批人同意即可' }
]

export const EMPTY_ACTIONS = [
  { label: '自动通过', value: 'AUTO_PASS' },
  { label: '转交超级管理员', value: 'TO_ADMIN' }
]

export const OPERATORS = [
  { label: '等于', value: 'EQ' },
  { label: '不等于', value: 'NE' },
  { label: '大于', value: 'GT' },
  { label: '大于等于', value: 'GE' },
  { label: '小于', value: 'LT' },
  { label: '小于等于', value: 'LE' },
  { label: '属于', value: 'IN' },
  { label: '包含', value: 'CONTAINS' }
]

/** 各字段类型可用的比较方式 */
const TYPE_OPERATORS = {
  NUMBER: ['EQ', 'NE', 'GT', 'GE', 'LT', 'LE', 'IN'],
  DATE: ['EQ', 'NE', 'GT', 'GE', 'LT', 'LE'],
  SELECT: ['EQ', 'NE', 'IN'],
  INPUT: ['EQ', 'NE', 'CONTAINS', 'IN'],
  TEXTAREA: ['EQ', 'NE', 'CONTAINS']
}

export function operatorOptions(fieldType) {
  const allowed = TYPE_OPERATORS[fieldType]
  return allowed ? OPERATORS.filter((o) => allowed.includes(o.value)) : OPERATORS
}

/** 下拉选项：表单字段的选项是字符串，业务变量的选项可以是 { label, value } */
export const toSelectOptions = (options = []) =>
  (options || []).map((o) => (typeof o === 'object' ? o : { label: o, value: o }))

const labelOf = (list, value) => list.find((i) => i.value === value)?.label || value

const ID_PATTERN = /^[A-Za-z][A-Za-z0-9_]{0,63}$/
const randomId = (prefix) => `${prefix}_${Math.random().toString(36).slice(2, 10).padEnd(8, '0')}`

export const DEFAULT_BRANCH_NAME = '其他情况'

export function createStartNode() {
  return { id: 'start', type: NODE_TYPES.START, name: '发起人', next: null }
}

export function createBranch(name, defaultBranch = false) {
  return { id: randomId('branch'), name, defaultBranch, conditions: defaultBranch ? [] : [[emptyRule()]], next: null }
}

export const emptyRule = () => ({ field: undefined, operator: 'EQ', value: '' })

export function createNode(type) {
  const base = { id: randomId('node'), type, next: null }
  switch (type) {
    case NODE_TYPES.APPROVER:
      return {
        ...base,
        name: '审批人',
        assignee: { type: 'USER', ids: [], names: [] },
        approveMode: 'OR',
        emptyAction: 'AUTO_PASS'
      }
    case NODE_TYPES.CC:
      return { ...base, name: '抄送人', assignee: { type: 'USER', ids: [], names: [] } }
    case NODE_TYPES.CONDITION:
      return { ...base, name: '条件分支', branches: [createBranch('条件1'), createBranch(DEFAULT_BRANCH_NAME, true)] }
    case NODE_TYPES.PARALLEL:
      return { ...base, name: '并行分支', branches: [createBranch('分支1'), createBranch('分支2')] }
    default:
      throw new Error(`未知节点类型：${type}`)
  }
}

/** 在 holder（前一个节点或分支）之后插入新节点；插入分支节点时，原后续节点接在分支汇合之后 */
export function insertNode(holder, type) {
  const node = createNode(type)
  node.next = holder.next
  holder.next = node
  return node
}

/** 删除 holder.next 节点，后续节点前移；分支节点连同各分支内的节点一起删除 */
export function removeNode(holder) {
  const node = holder.next
  if (node) holder.next = node.next
}

/** 新增条件 / 并行分支：条件分支插在默认分支之前，保证默认分支始终在最后 */
export function addBranch(node) {
  if (node.type === NODE_TYPES.CONDITION) {
    const count = node.branches.filter((b) => !b.defaultBranch).length
    node.branches.splice(node.branches.length - 1, 0, createBranch(`条件${count + 1}`))
  } else {
    node.branches.push(createBranch(`分支${node.branches.length + 1}`))
  }
}

/**
 * 删除分支；剩余分支不足 2 个时删除整个分支节点（holder 为分支节点的前一个节点 / 所在分支）
 */
export function removeBranch(holder, node, branch) {
  if (node.branches.length <= 2) {
    removeNode(holder)
    return
  }
  node.branches = node.branches.filter((b) => b !== branch)
}

/** 同类分支内左右移动（默认分支不参与） */
export function moveBranch(node, branch, offset) {
  const list = node.branches
  const i = list.indexOf(branch)
  const j = i + offset
  if (j < 0 || j >= list.length || list[j].defaultBranch || branch.defaultBranch) return
  ;[list[i], list[j]] = [list[j], list[i]]
}

/** 深度优先遍历所有节点：visit(node, branchOf)，branchOf 为节点所在的分支（顶层为 null） */
export function walk(node, visit, branchOf = null) {
  for (let n = node; n; n = n.next) {
    visit(n, branchOf)
    n.branches?.forEach((b) => walk(b.next, visit, b))
  }
}

const isCompleteRule = (r) => r && r.field && r.operator && r.value !== undefined && String(r.value).trim() !== ''

// ---------- 展示文案 ----------

export function assigneeText(node) {
  const a = node.assignee
  if (!a) return ''
  if (a.type === 'INITIATOR') return '发起人自己'
  if (a.type === 'DEPT_LEADER') return (a.level || 1) === 1 ? '直属主管' : `第 ${a.level} 级主管`
  if (!a.ids?.length) return ''
  const names = a.names?.length ? a.names : a.ids.map(String)
  const prefix = a.type === 'ROLE' ? '角色：' : ''
  return prefix + (names.length > 3 ? `${names.slice(0, 3).join('、')} 等 ${names.length} 人` : names.join('、'))
}

export function nodeSummary(node) {
  if (node.type === NODE_TYPES.APPROVER) {
    const who = assigneeText(node)
    if (!who) return ''
    const multi = !isSingleAssignee(node.assignee.type) && (node.assignee.type === 'ROLE' || node.assignee.ids.length > 1)
    return multi ? `${who}（${labelOf(APPROVE_MODES, node.approveMode)}）` : who
  }
  if (node.type === NODE_TYPES.CC) return assigneeText(node)
  return ''
}

/** 条件分支的条件文案：组内「且」，组间「或」 */
export function conditionText(branch, fields = []) {
  if (branch.defaultBranch) return '未满足其他条件时进入此分支'
  const fieldOf = (key) => fields.find((f) => f.field === key)
  const valueText = (r) => {
    const options = toSelectOptions(fieldOf(r.field)?.options)
    return String(r.value)
      .split(/[,，]/)
      .map((v) => labelOf(options, v.trim()))
      .join('、')
  }
  const groups = (branch.conditions || [])
    .map((g) =>
      g
        .filter(isCompleteRule)
        .map((r) => `${fieldOf(r.field)?.label || r.field} ${labelOf(OPERATORS, r.operator)} ${valueText(r)}`)
        .join(' 且 ')
    )
    .filter(Boolean)
  return groups.join(' 或 ')
}

// ---------- 校验 ----------

/**
 * 校验流程，返回 { errors: 错误提示[], invalidIds: Set<节点或分支 id> }
 * 与后端规则保持一致，后端仍会再次校验
 */
export function validateFlow(root, fields = []) {
  const errors = []
  const invalidIds = new Set()
  const ids = new Set()
  const fieldKeys = new Set(fields.map((f) => f.field))
  let approverCount = 0

  const fail = (id, msg) => {
    invalidIds.add(id)
    errors.push(msg)
  }
  const checkId = (id, label) => {
    if (!ID_PATTERN.test(id || '')) fail(id, `「${label}」的标识不合法`)
    else if (ids.has(id)) fail(id, `「${label}」的标识重复`)
    ids.add(id)
  }

  if (!root || root.type !== NODE_TYPES.START) {
    return { errors: ['流程缺少发起人节点'], invalidIds }
  }

  walk(root, (node) => {
    const name = node.name || '未命名节点'
    checkId(node.id, name)
    if (!node.name?.trim()) fail(node.id, '存在未填写名称的节点')

    if (node.type === NODE_TYPES.APPROVER || node.type === NODE_TYPES.CC) {
      if (node.type === NODE_TYPES.APPROVER) approverCount++
      const a = node.assignee
      if (!a?.type) fail(node.id, `「${name}」未设置${node.type === NODE_TYPES.CC ? '抄送人' : '审批人'}`)
      else if (requiresIds(a.type) && !a.ids?.length) {
        fail(node.id, `「${name}」未选择${a.type === 'ROLE' ? '角色' : '成员'}`)
      }
    }

    if (node.type === NODE_TYPES.CONDITION || node.type === NODE_TYPES.PARALLEL) {
      const branches = node.branches || []
      if (branches.length < 2) fail(node.id, `「${name}」至少需要 2 个分支`)
      if (node.type === NODE_TYPES.CONDITION && branches.filter((b) => b.defaultBranch).length !== 1) {
        fail(node.id, `「${name}」必须有且只有一个「${DEFAULT_BRANCH_NAME}」分支`)
      }
      branches.forEach((b) => {
        const bName = b.name || '未命名分支'
        checkId(b.id, bName)
        if (!b.name?.trim()) fail(b.id, `「${name}」存在未填写名称的分支`)
        if (node.type !== NODE_TYPES.CONDITION || b.defaultBranch) return
        const groups = b.conditions || []
        if (!groups.length || groups.some((g) => !g.length || !g.every(isCompleteRule))) {
          fail(b.id, `「${bName}」的条件未填写完整`)
        } else if (fieldKeys.size && groups.flat().some((r) => !fieldKeys.has(r.field))) {
          fail(b.id, `「${bName}」引用了表单中不存在的字段`)
        }
      })
    }
  })

  if (!approverCount) errors.unshift('流程至少需要一个审批人节点')
  return { errors, invalidIds }
}

/** 设计器上下文（provide / inject） */
export const DESIGNER_KEY = Symbol('flow-designer')
