<template>
  <a-tree-select
    :value="modelValue || undefined"
    :tree-data="treeData"
    :field-names="{ label: 'name', value: 'code', children: 'children' }"
    tree-node-label-prop="path"
    tree-node-filter-prop="path"
    :tree-default-expand-all="treeData.length <= 30"
    show-search
    allow-clear
    :placeholder="placeholder"
    :dropdown-match-select-width="false"
    :dropdown-style="{ maxHeight: '400px', minWidth: '220px', overflow: 'auto' }"
    @update:value="change"
  />
</template>

<script setup>
import { computed } from 'vue'
import { useMaterialCategories } from '@/composables/useMaterialCategories'
import { mapTree } from '@/utils/tree'

/**
 * 物料分类选择（树形），v-model 为分类编码，选中后显示完整路径。
 * - 筛选（默认）：可以选任意分类，后端按该分类及其下级分类筛选
 * - usable-only：表单中只能选启用的分类（停用的分类及其下级灰显；当前值即使停用也照常显示）
 */
const props = defineProps({
  modelValue: { type: String, default: undefined },
  placeholder: { type: String, default: undefined },
  usableOnly: Boolean
})
const emit = defineEmits(['update:modelValue', 'change'])

const { tree } = useMaterialCategories()

const treeData = computed(() =>
  props.usableOnly ? mapTree(tree.value, (n) => ({ ...n, disabled: !n.usable && n.code !== props.modelValue })) : tree.value
)

function change(v) {
  emit('update:modelValue', v ?? undefined)
  emit('change', v ?? undefined)
}
</script>
