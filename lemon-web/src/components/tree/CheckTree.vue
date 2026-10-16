<template>
  <div class="check-tree">
    <div class="check-tree-toolbar">
      <a-input v-model:value="keyword" placeholder="搜索" allow-clear size="small" class="check-tree-search">
        <template #prefix><SearchOutlined /></template>
      </a-input>
      <a-space :size="12" wrap>
        <a-checkbox :checked="allChecked" :indeterminate="someChecked" :disabled="disabled" @change="toggleAll">
          全选
        </a-checkbox>
        <a-checkbox :checked="expandedKeys.length > 0" @change="toggleExpand">展开</a-checkbox>
        <a-tooltip :title="linkageTip">
          <a-checkbox v-model:checked="linked" :disabled="disabled">父子联动</a-checkbox>
        </a-tooltip>
      </a-space>
    </div>
    <div class="check-tree-box" :style="{ maxHeight: height }">
      <a-tree
        v-if="treeData.length"
        v-model:expanded-keys="expandedKeys"
        :checked-keys="modelValue"
        :tree-data="treeData"
        :field-names="{ title: 'name', key: 'id', children: 'children' }"
        :disabled="disabled"
        checkable
        check-strictly
        block-node
        @check="onCheck"
      >
        <template #title="node">
          <HighlightText :text="node.name" :keyword="keyword" />
          <slot name="extra" v-bind="node" />
        </template>
      </a-tree>
      <a-empty v-else :image="Empty.PRESENTED_IMAGE_SIMPLE" :description="keyword ? '没有匹配的数据' : '暂无数据'" />
    </div>
    <div class="check-tree-footer">已选择 {{ modelValue.length }} / {{ allKeys.length }} 项</div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { Empty } from 'ant-design-vue'
import { SearchOutlined } from '@ant-design/icons-vue'
import HighlightText from './HighlightText.vue'
import { allIds, ancestorIds, filterTree, parentMap, selfAndDescendantIds } from '@/utils/tree'

/**
 * 可勾选的树（菜单授权、数据权限部门等）。
 * 使用 check-strictly 保证勾选结果与保存的 id 一一对应（例如「有页面权限但没有按钮权限」也能如实显示），
 * 父子联动由组件自行处理：
 * - 勾选：自身 + 子孙，checkAncestors 为 true 时同时勾选祖先（菜单：有按钮权限必须有页面权限）
 * - 取消：自身 + 子孙
 */
const props = defineProps({
  tree: { type: Array, default: () => [] },
  modelValue: { type: Array, default: () => [] },
  checkAncestors: { type: Boolean, default: false },
  disabled: Boolean,
  height: { type: String, default: '360px' },
  defaultExpandLevel: { type: Number, default: 1 }
})
const emit = defineEmits(['update:modelValue'])

const keyword = ref('')
const linked = ref(true)
const expandedKeys = ref([])

const allKeys = computed(() => allIds(props.tree))
const parents = computed(() => parentMap(props.tree))
const allChecked = computed(() => allKeys.value.length > 0 && props.modelValue.length === allKeys.value.length)
const someChecked = computed(() => props.modelValue.length > 0 && !allChecked.value)
const linkageTip = computed(() =>
  props.checkAncestors ? '勾选时同时勾选下级和上级，取消时同时取消下级' : '勾选或取消时同时处理全部下级'
)

const treeData = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return kw ? filterTree(props.tree, (n) => n.name.toLowerCase().includes(kw)) : props.tree
})

/** 展开前 defaultExpandLevel 层 */
function levelIds(nodes, level) {
  if (level <= 0) return []
  return nodes.flatMap((n) => (n.children?.length ? [n.id, ...levelIds(n.children, level - 1)] : []))
}
watch(
  () => props.tree,
  (tree) => (expandedKeys.value = levelIds(tree, props.defaultExpandLevel)),
  { immediate: true }
)
watch(keyword, (kw) => {
  expandedKeys.value = kw.trim() ? allIds(treeData.value) : levelIds(props.tree, props.defaultExpandLevel)
})

function onCheck(_, { checked, node }) {
  const set = new Set(props.modelValue)
  const ids = linked.value ? selfAndDescendantIds(props.tree, node.key) : [node.key]
  if (checked) {
    ids.forEach((id) => set.add(id))
    if (linked.value && props.checkAncestors) ancestorIds(parents.value, node.key).forEach((id) => set.add(id))
  } else {
    ids.forEach((id) => set.delete(id))
  }
  emit('update:modelValue', [...set])
}

function toggleAll(e) {
  emit('update:modelValue', e.target.checked ? [...allKeys.value] : [])
}

function toggleExpand(e) {
  expandedKeys.value = e.target.checked ? allIds(treeData.value) : []
}
</script>

<style scoped>
.check-tree-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}
.check-tree-search {
  width: 200px;
  max-width: 100%;
}
.check-tree-box {
  overflow: auto;
  border: 1px solid var(--app-border);
  border-radius: 8px;
  padding: 8px;
}
.check-tree-footer {
  margin-top: 6px;
  font-size: 12px;
  color: var(--app-text-3);
}
</style>
