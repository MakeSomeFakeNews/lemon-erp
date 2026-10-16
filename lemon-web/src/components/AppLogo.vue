<template>
  <svg :width="size" :height="size" viewBox="0 0 32 32" aria-hidden="true">
    <defs>
      <linearGradient :id="`${gid}-bg`" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0" stop-color="#ffd43b" />
        <stop offset="1" stop-color="#f59f00" />
      </linearGradient>
      <linearGradient :id="`${gid}-leaf`" x1="0" y1="1" x2="1" y2="0">
        <stop offset="0" stop-color="#2f9e44" />
        <stop offset="1" stop-color="#69db7c" />
      </linearGradient>
    </defs>
    <rect width="32" height="32" rx="8" :fill="`url(#${gid}-bg)`" />
    <!-- 柠檬切片：果皮、果肉、八瓣 -->
    <circle cx="15" cy="17.5" r="9.2" fill="#fff" />
    <circle cx="15" cy="17.5" r="7.6" fill="#fff3bf" />
    <g stroke="#fff" stroke-width="1.15" stroke-linecap="round">
      <path d="M15 10.4v14.2M7.9 17.5h14.2M10 12.5l10 10M20 12.5l-10 10" />
    </g>
    <circle cx="15" cy="17.5" r="1.5" fill="#fff" />
    <!-- 叶子 -->
    <path d="M21.2 9.6c.4-3.2 3.1-5.1 6.6-4.8-.3 3.4-2.9 5.6-6.6 4.8z" :fill="`url(#${gid}-leaf)`" />
    <path d="M21.4 9.4c1.6-1.2 3.2-2.3 5.2-3.3" stroke="#d3f9d8" stroke-width=".6" stroke-linecap="round" fill="none" />
  </svg>
</template>

<script setup>
import { useId } from 'vue'

defineProps({ size: { type: Number, default: 32 } })

// 同一页面可能有多个 Logo（其中一个被隐藏时共享的渐变会失效），每个实例使用独立的渐变 id
const gid = `app-logo-${useId()}`
</script>
