<template>
  <div class="kpi" :style="{ '--accent': color }">
    <div class="kpi-label">{{ label }}</div>
    <div class="kpi-value">
      <span class="num">{{ display }}</span><span v-if="unit" class="unit">{{ unit }}</span>
    </div>
    <div v-if="sub" class="kpi-sub" :class="subClass">{{ sub }}</div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'

/**
 * 指标卡：数值变化时滚动到新值
 * - value：数值（字符串则直接显示）；digits：小数位
 * - sub / subClass：副标题，如同比；subClass 为 up / down / warn
 */
const props = defineProps({
  label: String,
  value: [Number, String],
  unit: String,
  digits: { type: Number, default: 0 },
  sub: String,
  subClass: String,
  color: { type: String, default: '#00e4ff' }
})

const shown = ref(0)
let frame
function animate(to) {
  cancelAnimationFrame(frame)
  const from = shown.value
  const start = performance.now()
  const step = (t) => {
    const p = Math.min(1, (t - start) / 900)
    shown.value = from + (to - from) * (1 - Math.pow(1 - p, 3))
    if (p < 1) frame = requestAnimationFrame(step)
  }
  frame = requestAnimationFrame(step)
}
/** 数值或数字字符串（后端 BigDecimal）可以滚动显示，其他字符串原样显示 */
const numeric = (v) => (typeof v === 'number' ? v : typeof v === 'string' && v.trim() !== '' && !Number.isNaN(Number(v)) ? Number(v) : null)
watch(
  () => props.value,
  (v) => {
    const n = numeric(v)
    if (n !== null) animate(n)
  },
  { immediate: true }
)
onBeforeUnmount(() => cancelAnimationFrame(frame))

const display = computed(() => {
  const v = props.value
  if (v === null || v === undefined || v === '') return '-'
  if (numeric(v) === null) return v
  return shown.value.toLocaleString('zh-CN', { minimumFractionDigits: props.digits, maximumFractionDigits: props.digits })
})
</script>

<style scoped>
.kpi {
  position: relative;
  padding: 14px 18px;
  background: linear-gradient(135deg, rgba(14, 50, 100, 0.6), rgba(8, 26, 56, 0.4));
  border: 1px solid rgba(64, 158, 255, 0.22);
  overflow: hidden;
}
.kpi::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--accent);
  box-shadow: 0 0 12px var(--accent);
}
.kpi-label {
  font-size: 15px;
  color: rgba(160, 190, 230, 0.85);
  letter-spacing: 1px;
}
.kpi-value {
  margin-top: 6px;
  white-space: nowrap;
}
.num {
  font-family: 'DIN Alternate', 'Bahnschrift', 'Roboto Mono', 'SF Mono', Consolas, monospace;
  font-size: 34px;
  font-weight: 700;
  color: var(--accent);
  text-shadow: 0 0 12px color-mix(in srgb, var(--accent) 45%, transparent);
}
.unit {
  margin-left: 6px;
  font-size: 14px;
  color: rgba(160, 190, 230, 0.85);
}
.kpi-sub {
  margin-top: 2px;
  font-size: 13px;
  color: rgba(160, 190, 230, 0.75);
}
.kpi-sub.up { color: #2fe6a5; }
.kpi-sub.down { color: #ff5b5b; }
.kpi-sub.warn { color: #ffb03a; }
</style>
