<template>
  <component :is="icon" v-if="icon" />
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { iconModule, loadIcons } from '@/utils/icons'

/** 按名称渲染 Ant Design 图标，图标库异步加载 */
const props = defineProps({ name: String })
onMounted(() => props.name && loadIcons())
const icon = computed(() => (props.name && iconModule.value ? iconModule.value[props.name] : null))
</script>
