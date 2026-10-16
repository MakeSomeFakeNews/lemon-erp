<template>
  <div v-if="background" class="app-watermark" :style="{ backgroundImage: background }" aria-hidden="true" />
</template>

<script setup>
import { computed } from 'vue'
import dayjs from 'dayjs'
import { useConfigStore } from '@/store/config'
import { useUserStore } from '@/store/user'

/** 页面水印（参数设置开启时）：平铺当前用户的姓名、账号和日期，不影响点击 */
const configStore = useConfigStore()
const userStore = useUserStore()

const escape = (s) => String(s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c])

const background = computed(() => {
  const user = userStore.user
  if (!configStore.watermark || !user) return null
  const line1 = escape(`${user.nickname || ''} ${user.username}`.trim())
  const line2 = escape(dayjs().format('YYYY-MM-DD'))
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="260" height="160">`
    + `<g transform="rotate(-22 130 80)" fill="rgba(128,128,128,0.13)" font-size="14" font-family="sans-serif" text-anchor="middle">`
    + `<text x="130" y="76">${line1}</text><text x="130" y="96">${line2}</text></g></svg>`
  return `url("data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}")`
})
</script>

<style scoped>
.app-watermark {
  position: fixed;
  inset: 0;
  z-index: 2000;
  pointer-events: none;
  background-repeat: repeat;
}
</style>
