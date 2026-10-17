import { t } from '@/locales'

import { ApproveMode, AssigneeType } from '@/constants/enums'
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
  { label: t('workflow.assignee.user'), value: 'USER', requiresIds: true },
  { label: t('workflow.assignee.role'), value: 'ROLE', requiresIds: true },
  { label: t('workflow.assignee.deptLeader'), value: 'DEPT_LEADER', single: true },
  { label: t('workflow.assignee.initiator'), value: 'INITIATOR', single: true }
]

const assigneeTypeOf = (type) => ASSIGNEE_TYPES.find((t) => t.value === type) || {}
export const requiresIds = (type) => !!assigneeTypeOf(type).requiresIds
export const isSingleAssignee = (type) => !!assigneeTypeOf(type).single

/** 部门主管级别：从发起人所在部门向上数，所在部门为第 1 级 */
export const LEADER_LEVELS = Array.from({ length: 10 }, (_, i) => ({
  label: i === 0 ? t('workflow.leader.directWithDept') : t('workflow.leader.level', { n: i + 1 }),
  value: i + 1
}))

export const APPROVE_MODES = [
  { label: t('workflow.approveMode.sequential'), value: 'SEQUENTIAL', desc: t('workflow.approveMode.sequentialDesc') },
  { label: t('workflow.approveMode.and'), value: 'AND', desc: t('workflow.approveMode.andDesc') },
  { label: t('workflow.approveMode.or'), value: 'OR', desc: t('workflow.approveMode.orDesc') }
]

export const EMPTY_ACTIONS = [
  { label: t('workflow.emptyAction.autoPass'), value: 'AUTO_PASS' },
  { label: t('workflow.emptyAction.toAdmin'), value: 'TO_ADMIN' }
]

export const OPERATORS = [
  { label: t('workflow.operator.eq'), value: 'EQ' },
  { label: t('workflow.operator.ne'), value: 'NE' },
  { label: t('workflow.operator.gt'), value: 'GT' },
  { label: t('workflow.operator.ge'), value: 'GE' },
  { label: t('workflow.operator.lt'), value: 'LT' },
  { label: t('workflow.operator.le'), value: 'LE' },
  { label: t('workflow.operator.in'), value: 'IN' },
  { label: t('workflow.operator.contains'), value: 'CONTAINS' }
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

export const DEFAULT_BRANCH_NAME = t('workflow.node.defaultBranch')

export function createStartNode() {
  return { id: 'start', type: NODE_TYPES.START, name: t('workflow.node.start'), next: null }
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
        name: t('workflow.node.approver'),
        assignee: { type: AssigneeType.USER, ids: [], names: [] },
        approveMode: ApproveMode.OR,
        emptyAction: 'AUTO_PASS'
      }
    case NODE_TYPES.CC:
      return { ...base, name: t('workflow.node.cc'), assignee: { type: AssigneeType.USER, ids: [], names: [] } }
    case NODE_TYPES.CONDITION:
      return { ...base, name: t('workflow.node.condition'), branches: [createBranch(t('workflow.node.conditionN', { n: 1 })), createBranch(DEFAULT_BRANCH_NAME, true)] }
    case NODE_TYPES.PARALLEL:
      return { ...base, name: t('workflow.node.parallel'), branches: [createBranch(t('workflow.node.branchN', { n: 1 })), createBranch(t('workflow.node.branchN', { n: 2 }))] }
    default:
      throw new Error(t('workflow.node.unknownType', { type }))
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
    node.branches.splice(node.branches.length - 1, 0, createBranch(t('workflow.node.conditionN', { n: count + 1 })))
  } else {
    node.branches.push(createBranch(t('workflow.node.branchN', { n: node.branches.length + 1 })))
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
  if (a.type === AssigneeType.INITIATOR) return t('workflow.assignee.initiator')
  if (a.type === AssigneeType.DEPT_LEADER) return (a.level || 1) === 1 ? t('workflow.leader.direct') : t('workflow.leader.level', { n: a.level })
  if (!a.ids?.length) return ''
  const names = a.names?.length ? a.names : a.ids.map(String)
  const sep = t('workflow.text.listSep')
  const prefix = a.type === AssigneeType.ROLE ? t('workflow.text.rolePrefix') : ''
  return prefix + (names.length > 3 ? t('workflow.text.andMore', { names: names.slice(0, 3).join(sep), n: names.length }) : names.join(sep))
}

export function nodeSummary(node) {
  if (node.type === NODE_TYPES.APPROVER) {
    const who = assigneeText(node)
    if (!who) return ''
    const multi = !isSingleAssignee(node.assignee.type) && (node.assignee.type === AssigneeType.ROLE || node.assignee.ids.length > 1)
    return multi ? t('workflow.text.withMode', { who, mode: labelOf(APPROVE_MODES, node.approveMode) }) : who
  }
  if (node.type === NODE_TYPES.CC) return assigneeText(node)
  return ''
}

/** 条件分支的条件文案：组内「且」，组间「或」 */
export function conditionText(branch, fields = []) {
  if (branch.defaultBranch) return t('workflow.text.defaultBranchDesc')
  const fieldOf = (key) => fields.find((f) => f.field === key)
  const valueText = (r) => {
    const options = toSelectOptions(fieldOf(r.field)?.options)
    return String(r.value)
      .split(/[,，]/)
      .map((v) => labelOf(options, v.trim()))
      .join(t('workflow.text.listSep'))
  }
  const groups = (branch.conditions || [])
    .map((g) =>
      g
        .filter(isCompleteRule)
        .map((r) => `${fieldOf(r.field)?.label || r.field} ${labelOf(OPERATORS, r.operator)} ${valueText(r)}`)
        .join(t('workflow.text.and'))
    )
    .filter(Boolean)
  return groups.join(t('workflow.text.or'))
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
    if (!ID_PATTERN.test(id || '')) fail(id, t('workflow.validate.idInvalid', { name: label }))
    else if (ids.has(id)) fail(id, t('workflow.validate.idDuplicate', { name: label }))
    ids.add(id)
  }

  if (!root || root.type !== NODE_TYPES.START) {
    return { errors: [t('workflow.validate.noStart')], invalidIds }
  }

  walk(root, (node) => {
    const name = node.name || t('workflow.validate.unnamedNode')
    checkId(node.id, name)
    if (!node.name?.trim()) fail(node.id, t('workflow.validate.nodeNameEmpty'))

    if (node.type === NODE_TYPES.APPROVER || node.type === NODE_TYPES.CC) {
      if (node.type === NODE_TYPES.APPROVER) approverCount++
      const a = node.assignee
      if (!a?.type) fail(node.id, node.type === NODE_TYPES.CC ? t('workflow.validate.noCc', { name }) : t('workflow.validate.noApprover', { name }))
      else if (requiresIds(a.type) && !a.ids?.length) {
        fail(node.id, a.type === AssigneeType.ROLE ? t('workflow.validate.noRole', { name }) : t('workflow.validate.noUser', { name }))
      }
    }

    if (node.type === NODE_TYPES.CONDITION || node.type === NODE_TYPES.PARALLEL) {
      const branches = node.branches || []
      if (branches.length < 2) fail(node.id, t('workflow.validate.minBranches', { name }))
      if (node.type === NODE_TYPES.CONDITION && branches.filter((b) => b.defaultBranch).length !== 1) {
        fail(node.id, t('workflow.validate.oneDefault', { name, branch: DEFAULT_BRANCH_NAME }))
      }
      branches.forEach((b) => {
        const bName = b.name || t('workflow.validate.unnamedBranch')
        checkId(b.id, bName)
        if (!b.name?.trim()) fail(b.id, t('workflow.validate.branchNameEmpty', { name }))
        if (node.type !== NODE_TYPES.CONDITION || b.defaultBranch) return
        const groups = b.conditions || []
        if (!groups.length || groups.some((g) => !g.length || !g.every(isCompleteRule))) {
          fail(b.id, t('workflow.validate.conditionIncomplete', { name: bName }))
        } else if (fieldKeys.size && groups.flat().some((r) => !fieldKeys.has(r.field))) {
          fail(b.id, t('workflow.validate.fieldMissing', { name: bName }))
        }
      })
    }
  })

  if (!approverCount) errors.unshift(t('workflow.validate.noApproverNode'))
  return { errors, invalidIds }
}

/** 设计器上下文（provide / inject） */
export const DESIGNER_KEY = Symbol('flow-designer')
