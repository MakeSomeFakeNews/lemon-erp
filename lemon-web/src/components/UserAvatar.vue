<template>
  <a-avatar :size="size" :src="src || undefined" :style="src ? undefined : { backgroundColor: bg }" class="user-avatar">
    {{ src ? '' : initial }}
  </a-avatar>
</template>

<script setup>
import { computed } from 'vue'

/** 用户头像：有图片显示图片，否则显示名称首字，背景色由名称决定（同一个人颜色固定） */
const props = defineProps({
  src: String,
  name: { type: String, default: '' },
  size: { type: [Number, String], default: 'default' }
})

const COLORS = ['#2a78d6', '#1baf7a', '#eb6834', '#4a3aa7', '#d55181', '#008300', '#c98500']
const initial = computed(() => (props.name || '?').slice(0, 1).toUpperCase())
const bg = computed(() => {
  let hash = 0
  for (const ch of props.name || '') hash = (hash * 31 + ch.charCodeAt(0)) >>> 0
  return COLORS[hash % COLORS.length]
})
</script>

<style scoped>
.user-avatar {
  flex-shrink: 0;
  font-weight: 500;
}
</style>
