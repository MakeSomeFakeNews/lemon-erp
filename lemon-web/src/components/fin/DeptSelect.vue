<template>
  <a-tree-select
    :value="value ?? undefined"
    :tree-data="tree"
    :field-names="{ label: 'name', value: 'id', children: 'children' }"
    :placeholder="placeholder"
    allow-clear
    tree-default-expand-all
    show-search
    tree-node-filter-prop="name"
    @change="(v) => emit('update:value', v ?? null)"
  />
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { deptTree } from '@/api/system/dept'
import { cached } from './fin'

/** 部门选择（凭证分录辅助核算、资产使用部门、预算部门） */
defineProps({
  value: Number,
  placeholder: { type: String, default: '选择部门' }
})
const emit = defineEmits(['update:value'])

const tree = ref([])
onMounted(async () => {
  tree.value = await cached('dept-tree', deptTree)
})
</script>
