<template>
  <div class="flow-designer">
    <div class="flow-zoom">
      <a-button size="small" :disabled="zoom <= MIN_ZOOM" @click="setZoom(zoom - 10)">
        <template #icon><MinusOutlined /></template>
      </a-button>
      <span class="flow-zoom-value" :title="$t('workflow.designer.resetZoom')" @click="setZoom(100)">{{ zoom }}%</span>
      <a-button size="small" :disabled="zoom >= MAX_ZOOM" @click="setZoom(zoom + 10)">
        <template #icon><PlusOutlined /></template>
      </a-button>
    </div>
    <div v-if="hasStatus" class="flow-legend">
      <span><i class="legend-dot done" />{{ $t('workflow.status.done') }}</span>
      <span><i class="legend-dot running" />{{ $t('workflow.status.running') }}</span>
      <span><i class="legend-dot rejected" />{{ $t('workflow.status.rejected') }}</span>
      <span><i class="legend-dot pending" />{{ $t('workflow.status.pending') }}</span>
    </div>

    <div class="flow-scroll">
      <div v-if="modelValue" class="flow-canvas" :style="{ zoom: zoom / 100 }">
        <div class="node-card start" :class="statusClass(modelValue.id)">
          <div class="node-head"><UserOutlined /><span>{{ modelValue.name || $t('workflow.node.start') }}</span></div>
          <div class="node-body">{{ $t('workflow.designer.anyoneCanStart') }}</div>
        </div>
        <AddNodeButton :holder="modelValue" />
        <FlowNodeChain :holder="modelValue" />
        <div class="flow-end">
          <span class="flow-end-dot" />
          <span>{{ $t('workflow.designer.end') }}</span>
        </div>
      </div>
    </div>

    <NodeConfigDrawer v-model:open="configOpen" :target="configTarget" :fields="fields" :roles="roles" @ok="applyConfig" />
  </div>
</template>

<script setup>
import { computed, onMounted, provide, ref, toRef } from 'vue'
import { MinusOutlined, PlusOutlined, UserOutlined } from '@ant-design/icons-vue'
import AddNodeButton from './AddNodeButton.vue'
import FlowNodeChain from './FlowNodeChain.vue'
import NodeConfigDrawer from './NodeConfigDrawer.vue'
import { listIdentityRoles } from '@/api/workflow/identity'
import { DESIGNER_KEY, NODE_TYPES, validateFlow, walk } from './flow'

/**
 * 仿钉钉流程设计器。modelValue 为开始节点（整棵流程树），编辑时直接修改该对象。
 * - readonly + nodeStatus：审批详情中展示流程进度（DONE / RUNNING / REJECTED）
 * - showValidation：为 true 时标红未配置完整的节点
 */
const props = defineProps({
  modelValue: Object,
  fields: { type: Array, default: () => [] },
  readonly: Boolean,
  nodeStatus: { type: Object, default: null },
  showValidation: Boolean
})

const MIN_ZOOM = 50
const MAX_ZOOM = 150
const zoom = ref(100)
const setZoom = (v) => (zoom.value = Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, v)))

const invalidIds = computed(() =>
  props.showValidation && !props.readonly ? validateFlow(props.modelValue, props.fields).invalidIds : new Set()
)

const hasStatus = computed(() => props.readonly && !!props.nodeStatus && Object.keys(props.nodeStatus).length > 0)
const STATUS_CLASS = { DONE: 'status-done', RUNNING: 'status-running', REJECTED: 'status-rejected' }
function statusClass(id) {
  if (!hasStatus.value) return ''
  // 开始节点：流程已发起即视为完成
  if (id === props.modelValue?.id && !props.nodeStatus[id]) return STATUS_CLASS.DONE
  return STATUS_CLASS[props.nodeStatus[id]] || 'status-pending'
}

/** 分支没有单独的状态时，分支内有节点已执行即视为走过该分支 */
function branchStatusClass(branch) {
  if (!hasStatus.value) return ''
  if (props.nodeStatus[branch.id]) return STATUS_CLASS[props.nodeStatus[branch.id]] || ''
  let reached = false
  walk(branch.next, (n) => (reached ||= !!props.nodeStatus[n.id]))
  return reached ? STATUS_CLASS.DONE : 'status-pending'
}

// 节点配置
const configOpen = ref(false)
const configTarget = ref(null)
function openNode(node) {
  if (props.readonly || ![NODE_TYPES.APPROVER, NODE_TYPES.CC].includes(node.type)) return
  configTarget.value = { node }
  configOpen.value = true
}
function openBranch(node, branch) {
  if (props.readonly) return
  configTarget.value = { node, branch }
  configOpen.value = true
}
function applyConfig(draft) {
  const { node, branch } = configTarget.value
  Object.assign(branch || node, draft, { next: (branch || node).next })
}

provide(DESIGNER_KEY, {
  readonly: toRef(props, 'readonly'),
  fields: toRef(props, 'fields'),
  invalidIds,
  statusClass,
  branchStatusClass,
  openNode,
  openBranch
})

const roles = ref([])
onMounted(async () => {
  if (!props.readonly) roles.value = await listIdentityRoles()
})
</script>

<style scoped>
.flow-designer {
  --flow-line: #cacaca;
  position: relative;
  height: 100%;
  min-height: 320px;
  background: var(--app-bg);
  border-radius: 8px;
  overflow: hidden;
}
:global(html[data-theme='dark']) .flow-designer {
  --flow-line: #4a4a4e;
}
.flow-scroll {
  height: 100%;
  overflow: auto;
}
.flow-canvas {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: max-content;
  padding: 56px 40px 48px;
}
.flow-zoom {
  position: absolute;
  top: 12px;
  right: 16px;
  z-index: 5;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px;
  background: var(--app-card);
  border-radius: 6px;
  box-shadow: var(--app-shadow);
}
.flow-zoom-value {
  width: 48px;
  text-align: center;
  font-size: 12px;
  cursor: pointer;
  user-select: none;
}
.flow-legend {
  position: absolute;
  top: 12px;
  left: 16px;
  z-index: 5;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 4px 10px;
  font-size: 12px;
  color: var(--app-text-2);
  background: var(--app-card);
  border-radius: 6px;
  box-shadow: var(--app-shadow);
}
.legend-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  margin-right: 4px;
  border-radius: 50%;
}
.legend-dot.done {
  background: #52c41a;
}
.legend-dot.running {
  background: #1677ff;
}
.legend-dot.rejected {
  background: #ff4d4f;
}
.legend-dot.pending {
  background: var(--flow-line);
}

/* 开始节点（其余节点样式在 FlowNodeChain 中） */
.node-card.start {
  position: relative;
  z-index: 1;
  width: 220px;
  border-radius: 8px;
  background: var(--app-card);
  border: 1px solid transparent;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
.node-card.start .node-head {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 7px 7px 0 0;
  background: #576a95;
  color: #fff;
  font-size: 13px;
}
.node-card.start .node-body {
  padding: 12px;
  font-size: 13px;
  color: var(--app-text-2);
}
.node-card.start.status-done {
  border-color: #52c41a;
}
.flow-end {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: var(--app-text-3);
  font-size: 12px;
}
.flow-end-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--flow-line);
}
</style>
