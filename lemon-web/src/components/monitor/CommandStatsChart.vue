<template>
  <div class="cmd-chart">
    <a-empty v-if="!rows.length" :image="simpleImage" :description="$t('monitor.cache.noCommandStats')" />
    <div v-else class="rows" role="list" :aria-label="$t('monitor.cache.topCommands', { n: rows.length })">
      <a-tooltip v-for="r in rows" :key="r.name" placement="top">
        <template #title>
          <div class="tip-title">{{ r.name.toUpperCase() }}</div>
          <div>{{ $t('monitor.cache.callsValue', { n: formatNumber(r.calls) }) }}</div>
          <div>{{ $t('monitor.cache.usecValue', { n: formatUsec(r.usec) }) }}</div>
          <div>{{ $t('monitor.cache.avgValue', { n: r.usecPerCall.toFixed(2) }) }}</div>
        </template>
        <div class="row" role="listitem" tabindex="0">
          <span class="name">{{ r.name }}</span>
          <span class="track">
            <span class="bar" :style="{ width: `${(r.calls / max) * 100}%` }" />
          </span>
          <span class="value">{{ formatNumber(r.calls) }}</span>
        </div>
      </a-tooltip>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Empty } from 'ant-design-vue'
import { formatNumber } from '@/utils/format'

/**
 * Redis 命令调用次数：单系列横向条形图，数值直接标注在条末，悬停显示耗时
 */
const props = defineProps({
  data: { type: Array, default: () => [] },
  top: { type: Number, default: 10 }
})

const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE
const rows = computed(() => props.data.slice(0, props.top))
const max = computed(() => Math.max(1, ...rows.value.map((r) => r.calls)))

function formatUsec(usec) {
  if (usec >= 1e6) return `${(usec / 1e6).toFixed(2)} s`
  if (usec >= 1e3) return `${(usec / 1e3).toFixed(2)} ms`
  return `${usec} μs`
}
</script>

<style scoped>
.rows {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.row {
  display: grid;
  grid-template-columns: 96px 1fr 64px;
  align-items: center;
  gap: 12px;
  padding: 5px 8px;
  border-radius: 6px;
  outline: none;
}
.row:hover,
.row:focus-visible {
  background: var(--app-hover);
}
.name {
  color: var(--app-text-2);
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.track {
  height: 14px;
  display: flex;
  align-items: center;
}
.bar {
  display: block;
  height: 100%;
  min-width: 2px;
  background: var(--chart-series-1);
  /* 数据端圆角，基线端平直 */
  border-radius: 0 4px 4px 0;
}
.value {
  text-align: right;
  color: var(--app-text);
  font-variant-numeric: tabular-nums;
  font-size: 12px;
}
.tip-title {
  font-weight: 600;
  margin-bottom: 4px;
}
</style>
