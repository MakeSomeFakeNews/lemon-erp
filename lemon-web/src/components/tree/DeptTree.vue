<template>
  <div class="dept-tree">
    <div class="dept-tree-head">
      <a-input v-model:value="keyword" placeholder="搜索部门" allow-clear>
        <template #prefix><SearchOutlined /></template>
      </a-input>
      <a-tooltip :title="expandedKeys.length ? '折叠全部' : '展开全部'">
        <a-button @click="toggleExpand">
          <template #icon><NodeCollapseOutlined v-if="expandedKeys.length" /><NodeExpandOutlined v-else /></template>
        </a-button>
      </a-tooltip>
    </div>
    <div v-if="showAll" class="dept-tree-all" :class="{ active: modelValue == null }" @click="select(undefined)">
      <ApartmentOutlined />
      <span>全部部门</span>
    </div>
    <div class="dept-tree-body">
      <a-spin :spinning="loading">
        <a-tree
          v-if="treeData.length"
          v-model:expanded-keys="expandedKeys"
          :tree-data="treeData"
          :field-names="{ title: 'name', key: 'id', children: 'children' }"
          :selected-keys="modelValue == null ? [] : [modelValue]"
          block-node
          @select="(keys) => select(keys[0])"
        >
          <template #title="node">
            <div class="dept-node">
              <HighlightText :text="node.name" :keyword="keyword" :class="['dept-node-name', { 'is-disabled': node.status === 0 }]" />
              <a-tag v-if="node.status === 0" :bordered="false" class="dept-node-tag">停用</a-tag>
              <span v-if="$slots.actions" class="dept-node-actions" @click.stop>
                <slot name="actions" :node="node.dataRef" />
              </span>
            </div>
          </template>
        </a-tree>
        <a-empty v-else :image="Empty.PRESENTED_IMAGE_SIMPLE" :description="keyword ? '没有匹配的部门' : '暂无部门'" />
      </a-spin>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { Empty } from 'ant-design-vue'
import { ApartmentOutlined, NodeCollapseOutlined, NodeExpandOutlined, SearchOutlined } from '@ant-design/icons-vue'
import HighlightText from './HighlightText.vue'
import { allIds, filterTree } from '@/utils/tree'

/**
 * 部门树：搜索高亮、展开折叠；v-model 为选中的部门 id
 * - showAll：显示「全部部门」，点击取消筛选
 * - #actions="{ node }"：节点右侧的操作区，鼠标悬停或选中时显示
 */
const props = defineProps({
  tree: { type: Array, default: () => [] },
  modelValue: { type: Number, default: undefined },
  loading: Boolean,
  showAll: { type: Boolean, default: true }
})
const emit = defineEmits(['update:modelValue', 'change'])

const keyword = ref('')
const expandedKeys = ref([])

const treeData = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return kw ? filterTree(props.tree, (n) => n.name.toLowerCase().includes(kw)) : props.tree
})

// 数据加载后、搜索时展开全部
watch(treeData, (data) => (expandedKeys.value = allIds(data)), { immediate: true })

function toggleExpand() {
  expandedKeys.value = expandedKeys.value.length ? [] : allIds(treeData.value)
}

function select(id) {
  // 再次点击已选中的部门时 antd 会传 undefined：有「全部部门」时等同于取消筛选，否则保持选中
  if (id === props.modelValue || (id === undefined && !props.showAll)) return
  emit('update:modelValue', id)
  emit('change', id)
}
</script>

<style scoped>
.dept-tree {
  display: flex;
  flex-direction: column;
  gap: 8px;
  height: 100%;
  min-height: 0;
}
.dept-tree-head {
  display: flex;
  gap: 8px;
}
.dept-tree-all {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}
.dept-tree-all:hover {
  background: var(--app-hover);
}
.dept-tree-all.active {
  color: var(--app-primary);
  background: var(--app-primary-bg);
}
.dept-tree-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
}
.dept-node {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
}
.dept-node-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.dept-node-tag {
  margin: 0;
  font-size: 11px;
  line-height: 16px;
  padding: 0 4px;
}
.dept-node-actions {
  display: none;
  flex: none;
}
:deep(.ant-tree-treenode:hover) .dept-node-actions,
:deep(.ant-tree-treenode-selected) .dept-node-actions {
  display: inline-flex;
}
.is-disabled {
  color: var(--app-text-3);
}
</style>
