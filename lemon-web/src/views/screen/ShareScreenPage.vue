<template>
  <ScreenRunner
    v-if="info"
    :screens="info.screens"
    :interval="info.intervalSeconds"
    :fetcher="(key) => shareData(token, key)"
    :company="info.company"
    :title-suffix="info.workCenterName"
  />
  <div v-else class="share-state">
    <div class="state-title">{{ error ? '无法打开大屏' : '正在打开大屏…' }}</div>
    <div v-if="error" class="state-msg">{{ error }}</div>
    <div v-if="error" class="state-msg sub">{{ retryIn }} 秒后重试</div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { shareData, shareInfo } from '@/api/bi/screen'
import ScreenRunner from './ScreenRunner.vue'

/** 投屏链接：/screen-share/{令牌}，免登录；链接失效或网络中断时每 30 秒重试（电视开机时网络可能还没连上） */
const route = useRoute()
const token = String(route.params.token || '')
const info = ref()
const error = ref('')
const retryIn = ref(30)

let timer
async function open() {
  try {
    info.value = await shareInfo(token)
    error.value = ''
    clearInterval(timer)
  } catch (e) {
    error.value = e.message
    retryIn.value = 30
    clearInterval(timer)
    timer = setInterval(() => {
      retryIn.value -= 1
      if (retryIn.value <= 0) open()
    }, 1000)
  }
}
onMounted(open)
onBeforeUnmount(() => clearInterval(timer))
</script>

<style scoped>
.share-state {
  position: fixed;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: radial-gradient(ellipse at 50% 0%, #0b2a5a 0%, #050f24 55%, #030914 100%);
  color: #cfe8ff;
}
.state-title {
  font-size: 32px;
  font-weight: 700;
  letter-spacing: 4px;
}
.state-msg {
  font-size: 18px;
  color: #ff8a8a;
}
.state-msg.sub {
  font-size: 14px;
  color: rgba(160, 190, 230, 0.7);
}
</style>
