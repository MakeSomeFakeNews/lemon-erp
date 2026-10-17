<template>
  <div class="add-node" :class="{ readonly: ctx.readonly.value }">
    <a-popover v-if="!ctx.readonly.value" v-model:open="open" trigger="click" placement="rightTop">
      <template #content>
        <div class="add-node-menu">
          <div v-for="item in ITEMS" :key="item.type" class="add-node-item" @click="add(item.type)">
            <span class="add-node-icon" :style="{ color: item.color, background: item.bg }">
              <component :is="item.icon" />
            </span>
            <span>{{ item.label }}</span>
          </div>
        </div>
      </template>
      <a-button type="primary" shape="circle" size="small" class="add-node-btn">
        <template #icon><PlusOutlined /></template>
      </a-button>
    </a-popover>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { inject, ref } from 'vue'
import { ApartmentOutlined, AuditOutlined, BranchesOutlined, SendOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { DESIGNER_KEY, NODE_TYPES, insertNode } from './flow'

const { t } = useI18n()

/** 节点之间的连线和「+」按钮，在 holder（前一个节点或分支）之后插入节点 */
const props = defineProps({ holder: { type: Object, required: true } })
const ctx = inject(DESIGNER_KEY)
const open = ref(false)

const ITEMS = [
  { type: NODE_TYPES.APPROVER, label: t('workflow.node.approver'), icon: AuditOutlined, color: '#ff943e', bg: 'rgba(255, 148, 62, 0.12)' },
  { type: NODE_TYPES.CC, label: t('workflow.node.cc'), icon: SendOutlined, color: '#3296fa', bg: 'rgba(50, 150, 250, 0.12)' },
  { type: NODE_TYPES.CONDITION, label: t('workflow.node.condition'), icon: BranchesOutlined, color: '#15bc83', bg: 'rgba(21, 188, 131, 0.12)' },
  { type: NODE_TYPES.PARALLEL, label: t('workflow.node.parallel'), icon: ApartmentOutlined, color: '#718dff', bg: 'rgba(113, 141, 255, 0.12)' }
]

function add(type) {
  open.value = false
  const node = insertNode(props.holder, type)
  if (type === NODE_TYPES.APPROVER || type === NODE_TYPES.CC) ctx.openNode(node)
}
</script>

<style scoped>
.add-node {
  position: relative;
  width: 240px;
  min-height: 72px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
}
.add-node.readonly {
  min-height: 40px;
}
.add-node::before {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: 50%;
  width: 2px;
  transform: translateX(-50%);
  background: var(--flow-line);
}
.add-node-btn {
  position: relative;
  z-index: 1;
  box-shadow: 0 2px 6px rgba(22, 119, 255, 0.35);
  transition: transform 0.15s;
}
.add-node-btn:hover {
  transform: scale(1.15);
}
.add-node-menu {
  display: grid;
  grid-template-columns: repeat(2, 120px);
  gap: 8px;
}
.add-node-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
}
.add-node-item:hover {
  border-color: var(--app-primary);
  background: var(--app-hover);
}
.add-node-icon {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
}
</style>
