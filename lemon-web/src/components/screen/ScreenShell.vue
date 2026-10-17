<template>
  <div ref="root" class="screen-root" @mousemove="showTools">
    <div class="screen-canvas" :style="canvasStyle">
      <header class="screen-head">
        <div class="head-side left">
          <div class="company">{{ company }}</div>
          <div class="date">{{ dateText }}</div>
        </div>
        <div class="head-title">
          <div class="title">{{ title }}</div>
          <div v-if="subtitle" class="subtitle">{{ subtitle }}</div>
        </div>
        <div class="head-side right">
          <div class="clock">{{ clockText }}</div>
          <div class="status">
            <span v-if="error" class="offline">● {{ error }}</span>
            <span v-else-if="updatedAt">{{ $t('screen.shell.updatedAt', { time: updatedAt }) }}</span>
          </div>
        </div>
      </header>
      <div v-if="screens.length > 1" class="nav-dots">
        <span v-for="(s, i) in screens" :key="s.key" :class="['dot', { active: i === current }]" @click="emit('switch', i)">{{ s.label }}</span>
      </div>
      <main class="screen-body"><slot /></main>
    </div>
    <div :class="['tools', { visible: toolsVisible }]">
      <button v-if="screens.length > 1" type="button" @click="emit('toggle-play')">{{ playing ? $t('screen.shell.pause') : $t('screen.shell.resume') }}</button>
      <button type="button" @click="toggleFullscreen">{{ fullscreen ? $t('screen.shell.exitFullscreen') : $t('screen.shell.fullscreen') }}</button>
      <button v-if="closable" type="button" @click="emit('close')">{{ $t('common.action.back') }}</button>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { useI18n } from 'vue-i18n'
import { DESIGN_HEIGHT, DESIGN_WIDTH } from './screen'

/**
 * 大屏外壳：按 1920 × 1080 设计，等比缩放铺满窗口（居中，留边为背景色）；
 * 顶部标题栏（企业名称、日期、时钟、数据更新时间 / 离线提示），多屏时显示切换按钮；
 * 鼠标移动时显示右下角的轮播、全屏、返回按钮
 */
defineProps({
  title: String,
  subtitle: String,
  company: String,
  updatedAt: String,
  error: String,
  screens: { type: Array, default: () => [] },
  current: { type: Number, default: 0 },
  playing: Boolean,
  closable: Boolean
})
const emit = defineEmits(['switch', 'toggle-play', 'close'])
const { t } = useI18n()

const root = ref()
const scale = ref(1)
const now = ref(dayjs())
const fullscreen = ref(false)
const toolsVisible = ref(false)

const canvasStyle = computed(() => ({ width: `${DESIGN_WIDTH}px`, height: `${DESIGN_HEIGHT}px`, transform: `translate(-50%, -50%) scale(${scale.value})` }))
const dateText = computed(() => {
  const d = now.value
  return t('screen.shell.date', { date: d.format(t('screen.shell.dateFormat')), week: t(`screen.week.${d.day()}`) })
})
const clockText = computed(() => now.value.format('HH:mm:ss'))

function resize() {
  scale.value = Math.min(window.innerWidth / DESIGN_WIDTH, window.innerHeight / DESIGN_HEIGHT)
}
function toggleFullscreen() {
  if (document.fullscreenElement) document.exitFullscreen()
  else root.value.requestFullscreen?.()
}
function onFullscreenChange() {
  fullscreen.value = !!document.fullscreenElement
}
let hideTimer
function showTools() {
  toolsVisible.value = true
  clearTimeout(hideTimer)
  hideTimer = setTimeout(() => (toolsVisible.value = false), 2500)
}

let clock
onMounted(() => {
  resize()
  window.addEventListener('resize', resize)
  document.addEventListener('fullscreenchange', onFullscreenChange)
  clock = setInterval(() => (now.value = dayjs()), 1000)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  document.removeEventListener('fullscreenchange', onFullscreenChange)
  clearInterval(clock)
  clearTimeout(hideTimer)
})
</script>

<style scoped>
.screen-root {
  position: fixed;
  inset: 0;
  overflow: hidden;
  background: radial-gradient(ellipse at 50% 0%, #0b2a5a 0%, #050f24 55%, #030914 100%);
  color: rgba(225, 238, 255, 0.92);
  font-family: -apple-system, 'PingFang SC', 'Microsoft YaHei', 'Noto Sans SC', sans-serif;
  user-select: none;
}
.screen-canvas {
  position: absolute;
  left: 50%;
  top: 50%;
  display: flex;
  flex-direction: column;
  background-image:
    linear-gradient(rgba(0, 140, 255, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 140, 255, 0.05) 1px, transparent 1px);
  background-size: 40px 40px;
}
.screen-head {
  position: relative;
  flex: none;
  height: 88px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  background: linear-gradient(180deg, rgba(0, 100, 220, 0.28), transparent);
}
.screen-head::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, #00e4ff 30%, #00e4ff 70%, transparent);
  opacity: 0.6;
}
.head-side {
  width: 460px;
}
.head-side.right {
  text-align: right;
}
.company {
  font-size: 20px;
  font-weight: 600;
  color: #cfe8ff;
}
.date,
.status {
  margin-top: 4px;
  font-size: 15px;
  color: rgba(160, 190, 230, 0.8);
}
.clock {
  font-family: 'DIN Alternate', 'Bahnschrift', 'Roboto Mono', 'SF Mono', Consolas, monospace;
  font-size: 30px;
  font-weight: 700;
  color: #00e4ff;
  letter-spacing: 2px;
}
.offline {
  color: #ff5b5b;
}
.head-title {
  text-align: center;
}
.title {
  font-size: 40px;
  font-weight: 800;
  letter-spacing: 8px;
  background: linear-gradient(180deg, #ffffff, #7fd8ff);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  text-shadow: 0 0 24px rgba(0, 200, 255, 0.35);
}
.subtitle {
  font-size: 15px;
  color: rgba(160, 190, 230, 0.85);
  letter-spacing: 2px;
}
.nav-dots {
  position: absolute;
  top: 92px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 10px;
  z-index: 2;
}
.dot {
  padding: 2px 14px;
  font-size: 13px;
  color: rgba(160, 190, 230, 0.75);
  border: 1px solid rgba(64, 158, 255, 0.35);
  cursor: pointer;
}
.dot.active {
  color: #04122a;
  background: #00e4ff;
  border-color: #00e4ff;
}
.screen-body {
  flex: 1;
  min-height: 0;
  padding: 26px 24px 20px;
}
.tools {
  position: fixed;
  right: 20px;
  bottom: 20px;
  display: flex;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.3s;
  z-index: 10;
}
.tools.visible {
  opacity: 1;
}
.tools button {
  padding: 6px 14px;
  font-size: 13px;
  color: #cfe8ff;
  background: rgba(8, 30, 64, 0.85);
  border: 1px solid rgba(0, 228, 255, 0.45);
  cursor: pointer;
}
.tools button:hover {
  color: #04122a;
  background: #00e4ff;
}
</style>
