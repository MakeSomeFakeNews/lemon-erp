<template>
  <div class="scroll-list">
    <div class="row head" :style="gridStyle">
      <span v-for="c in columns" :key="c.key" :style="{ textAlign: c.align || 'left' }">{{ c.title }}</span>
    </div>
    <div ref="viewport" class="viewport">
      <div v-if="!rows.length" class="empty">{{ emptyText }}</div>
      <div class="track" :style="{ transform: `translateY(${-offset * rowHeight}px)`, transition: animating ? 'transform 0.6s ease' : 'none' }">
        <div v-for="(r, i) in looped" :key="i" class="row" :class="{ odd: i % 2 === 1 }" :style="{ ...gridStyle, height: `${rowHeight}px` }">
          <span v-for="c in columns" :key="c.key" class="cell" :style="{ textAlign: c.align || 'left' }">
            <slot name="cell" :column="c" :row="r">{{ r[c.key] ?? '' }}</slot>
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'

/**
 * 自动滚动列表：行数超过可视区域时每隔 interval 毫秒上移一行，首尾相接循环
 * - columns：[{ key, title, width（fr 或 px，默认 1fr）, align }]
 * - rows：数据；单元格内容用 #cell="{ column, row }" 自定义
 */
const props = defineProps({
  columns: { type: Array, required: true },
  rows: { type: Array, default: () => [] },
  rowHeight: { type: Number, default: 40 },
  interval: { type: Number, default: 3000 },
  emptyText: { type: String, default: '暂无数据' }
})

const viewport = ref()
const visibleRows = ref(0)
const offset = ref(0)
const animating = ref(true)
const gridStyle = computed(() => ({ gridTemplateColumns: props.columns.map((c) => c.width || '1fr').join(' ') }))
const scrolling = computed(() => props.rows.length > visibleRows.value)
/** 滚动时把列表复制一份接在后面，滚到第一份末尾时无动画跳回开头 */
const looped = computed(() => (scrolling.value ? [...props.rows, ...props.rows] : props.rows))

let timer
let observer
function tick() {
  if (!scrolling.value) {
    offset.value = 0
    return
  }
  animating.value = true
  offset.value += 1
  if (offset.value >= props.rows.length) {
    setTimeout(() => {
      animating.value = false
      offset.value = 0
    }, 650)
  }
}
function measure() {
  visibleRows.value = viewport.value ? Math.floor(viewport.value.clientHeight / props.rowHeight) : 0
}
onMounted(() => {
  measure()
  observer = new ResizeObserver(measure)
  observer.observe(viewport.value)
  timer = setInterval(tick, props.interval)
})
onBeforeUnmount(() => {
  clearInterval(timer)
  observer?.disconnect()
})
watch(() => props.rows.length, (n) => {
  if (offset.value >= n) offset.value = 0
})
</script>

<style scoped>
.scroll-list {
  display: flex;
  flex-direction: column;
  height: 100%;
  font-size: 15px;
  color: rgba(225, 238, 255, 0.92);
}
.row {
  display: grid;
  align-items: center;
  column-gap: 10px;
  padding: 0 12px;
}
.head {
  height: 36px;
  flex: none;
  font-size: 14px;
  color: #7fd8ff;
  background: rgba(0, 140, 255, 0.16);
}
.viewport {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  position: relative;
}
.row.odd {
  background: rgba(0, 120, 255, 0.07);
}
.cell {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(160, 190, 230, 0.6);
}
</style>
