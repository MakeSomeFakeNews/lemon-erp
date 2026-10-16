<template>
  <template v-if="node">
    <!-- 审批人 / 抄送人 -->
    <template v-if="!isBranchNode">
      <div :class="cardClass" @click="ctx.openNode(node)">
        <div class="node-head">
          <component :is="meta.icon" />
          <span class="node-title">{{ node.name }}</span>
          <CloseOutlined v-if="!readonly" class="node-close" title="删除节点" @click.stop="remove" />
        </div>
        <div class="node-body">
          <span v-if="summary" class="node-summary">{{ summary }}</span>
          <span v-else class="node-placeholder">{{ meta.placeholder }}</span>
          <RightOutlined v-if="!readonly" class="node-arrow" />
        </div>
        <ExclamationCircleFilled v-if="invalid" class="node-invalid-icon" />
      </div>
      <AddNodeButton :holder="node" />
    </template>

    <!-- 条件分支 / 并行分支 -->
    <template v-else>
      <div class="branch-wrap">
        <a-button v-if="!readonly" size="small" class="branch-add" @click="addBranchItem">
          {{ isCondition ? '添加条件' : '添加分支' }}
        </a-button>
        <span v-else class="branch-add branch-label">{{ node.name }}</span>
        <div class="branch-cols">
          <div v-for="(b, i) in node.branches" :key="b.id" class="branch-col">
            <div class="branch-line" />
            <div :class="branchClass(b)" @click="!b.defaultBranch && ctx.openBranch(node, b)">
              <div class="branch-head">
                <span class="branch-name">{{ b.name }}</span>
                <span v-if="isCondition" class="branch-priority">优先级 {{ i + 1 }}</span>
                <CloseOutlined
                  v-if="!readonly && !b.defaultBranch"
                  class="node-close"
                  title="删除分支"
                  @click.stop="removeBranchItem(b)"
                />
              </div>
              <div class="branch-body">
                <template v-if="isCondition">
                  <span v-if="conditionText(b, ctx.fields.value)">{{ conditionText(b, ctx.fields.value) }}</span>
                  <span v-else class="node-placeholder">请设置条件</span>
                </template>
                <span v-else class="node-placeholder">{{ readonly ? '同时执行' : '无需设置条件，同时执行' }}</span>
              </div>
              <div v-if="!readonly && !b.defaultBranch && movable(i)" class="branch-move">
                <LeftOutlined v-if="canMove(i, -1)" title="左移" @click.stop="move(b, -1)" />
                <RightOutlined v-if="canMove(i, 1)" title="右移" @click.stop="move(b, 1)" />
              </div>
              <ExclamationCircleFilled v-if="ctx.invalidIds.value.has(b.id)" class="node-invalid-icon" />
            </div>
            <AddNodeButton :holder="b" />
            <FlowNodeChain :holder="b" />
          </div>
        </div>
      </div>
      <AddNodeButton :holder="node" />
    </template>

    <FlowNodeChain :holder="node" />
  </template>
</template>

<script setup>
import { computed, inject } from 'vue'
import { Modal } from 'ant-design-vue'
import {
  AuditOutlined,
  CloseOutlined,
  ExclamationCircleFilled,
  LeftOutlined,
  RightOutlined,
  SendOutlined
} from '@ant-design/icons-vue'
import AddNodeButton from './AddNodeButton.vue'
import { DESIGNER_KEY, NODE_TYPES, addBranch, conditionText, moveBranch, nodeSummary, removeBranch, removeNode } from './flow'

/** 递归渲染 holder.next 开始的节点链 */
const props = defineProps({ holder: { type: Object, required: true } })
const ctx = inject(DESIGNER_KEY)

const node = computed(() => props.holder.next)
const readonly = computed(() => ctx.readonly.value)
const isBranchNode = computed(() => node.value.type === NODE_TYPES.CONDITION || node.value.type === NODE_TYPES.PARALLEL)
const isCondition = computed(() => node.value.type === NODE_TYPES.CONDITION)
const invalid = computed(() => ctx.invalidIds.value.has(node.value.id))
const summary = computed(() => nodeSummary(node.value))

const META = {
  [NODE_TYPES.APPROVER]: { icon: AuditOutlined, placeholder: '请设置审批人', cls: 'approver' },
  [NODE_TYPES.CC]: { icon: SendOutlined, placeholder: '请设置抄送人', cls: 'cc' }
}
const meta = computed(() => META[node.value.type] || META[NODE_TYPES.APPROVER])

const cardClass = computed(() => [
  'node-card',
  meta.value.cls,
  ctx.statusClass(node.value.id),
  { invalid: invalid.value, clickable: !readonly.value }
])

const branchClass = (b) => [
  'branch-card',
  ctx.branchStatusClass(b),
  { invalid: ctx.invalidIds.value.has(b.id), clickable: !readonly.value && !b.defaultBranch, default: b.defaultBranch }
]

const remove = () => removeNode(props.holder)
const addBranchItem = () => addBranch(node.value)

function removeBranchItem(branch) {
  const removeAll = node.value.branches.length <= 2
  const doRemove = () => removeBranch(props.holder, node.value, branch)
  const hasChildren = !!branch.next
  if (!removeAll && !hasChildren) return doRemove()
  Modal.confirm({
    title: removeAll ? `删除整个「${node.value.name}」？` : `删除「${branch.name}」？`,
    content: removeAll ? '分支少于 2 个时将删除整个分支节点及其中的所有节点。' : '该分支下的所有节点将一并删除。',
    okType: 'danger',
    onOk: doRemove
  })
}

// 默认分支固定在最后，不参与移动
const movableCount = computed(() => node.value.branches.filter((b) => !b.defaultBranch).length)
const movable = () => movableCount.value > 1
const canMove = (i, offset) => {
  const j = i + offset
  return j >= 0 && j < node.value.branches.length && !node.value.branches[j].defaultBranch
}
const move = (branch, offset) => moveBranch(node.value, branch, offset)
</script>

<style scoped>
.node-card,
.branch-card {
  position: relative;
  z-index: 1;
  width: 220px;
  border-radius: 8px;
  background: var(--app-card);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  border: 1px solid transparent;
  text-align: left;
  transition: box-shadow 0.2s, border-color 0.2s, opacity 0.2s;
  flex: none;
}
.node-card.clickable,
.branch-card.clickable {
  cursor: pointer;
}
.node-card.clickable:hover,
.branch-card.clickable:hover {
  border-color: var(--app-primary);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}
.node-head {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 7px 7px 0 0;
  color: #fff;
  font-size: 13px;
}
.node-card.approver .node-head {
  background: #ff943e;
}
.node-card.cc .node-head {
  background: #3296fa;
}
.node-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.node-close {
  font-size: 12px;
  opacity: 0;
  transition: opacity 0.2s;
}
.node-card:hover .node-close,
.branch-card:hover .node-close {
  opacity: 1;
}
.node-body,
.branch-body {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  min-height: 46px;
  color: var(--app-text);
  font-size: 13px;
  line-height: 1.5;
  word-break: break-all;
}
.node-summary {
  flex: 1;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.node-placeholder {
  flex: 1;
  color: var(--app-text-3);
}
.node-arrow {
  color: var(--app-text-3);
  font-size: 12px;
}
.invalid {
  border-color: #ff4d4f !important;
}
.node-invalid-icon {
  position: absolute;
  top: 50%;
  right: -26px;
  transform: translateY(-50%);
  color: #ff4d4f;
  font-size: 18px;
}

/* 节点状态（审批详情的只读流程图） */
.status-done {
  border-color: #52c41a;
}
.status-running {
  border-color: #1677ff;
  box-shadow: 0 0 0 3px rgba(22, 119, 255, 0.2);
}
.status-rejected {
  border-color: #ff4d4f;
}
.status-pending {
  opacity: 0.55;
}

/* 分支 */
.branch-wrap {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: none;
}
.branch-add {
  position: absolute;
  top: 0;
  left: 50%;
  z-index: 2;
  transform: translate(-50%, -50%);
  border-radius: 14px;
  color: #15bc83;
  border-color: #15bc83;
}
.branch-label {
  padding: 2px 12px;
  font-size: 12px;
  background: var(--app-card);
  border: 1px solid #15bc83;
  white-space: nowrap;
}
.branch-cols {
  display: flex;
}
.branch-col {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 36px 20px 24px;
}
/* 分支顶部、底部的横线：首列从中间开始，末列到中间结束 */
.branch-col::before,
.branch-col::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  height: 2px;
  background: var(--flow-line);
}
.branch-col::before {
  top: 0;
}
.branch-col::after {
  bottom: 0;
}
.branch-col:first-child::before,
.branch-col:first-child::after {
  left: 50%;
}
.branch-col:last-child::before,
.branch-col:last-child::after {
  right: 50%;
}
.branch-line {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 50%;
  width: 2px;
  transform: translateX(-50%);
  background: var(--flow-line);
}
.branch-head {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px 0;
  font-size: 13px;
}
.branch-name {
  flex: 1;
  min-width: 0;
  color: #15bc83;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.branch-card.default .branch-name {
  color: var(--app-text-2);
}
.branch-priority {
  color: var(--app-text-3);
  font-size: 12px;
}
.branch-card .node-close {
  color: var(--app-text-3);
}
.branch-body {
  padding-top: 6px;
  min-height: 40px;
}
.branch-move {
  position: absolute;
  bottom: 4px;
  right: 8px;
  display: flex;
  gap: 8px;
  color: var(--app-text-3);
  font-size: 12px;
  opacity: 0;
  transition: opacity 0.2s;
}
.branch-card:hover .branch-move {
  opacity: 1;
}
.branch-move :deep(.anticon:hover) {
  color: var(--app-primary);
}
</style>
