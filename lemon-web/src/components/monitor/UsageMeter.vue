<template>
  <div class="meter">
    <div class="meter-head">
      <span class="meter-label">{{ label }}</span>
      <span class="meter-value">
        <span v-if="level !== 'normal'" class="meter-state" :class="level">
          <WarningFilled v-if="level === 'warning'" />
          <CloseCircleFilled v-else />
          {{ level === 'warning' ? '偏高' : '告警' }}
        </span>
        {{ value.toFixed(1) }}%
      </span>
    </div>
    <div
      class="meter-track"
      role="meter"
      :aria-label="label"
      aria-valuemin="0"
      aria-valuemax="100"
      :aria-valuenow="value"
    >
      <div class="meter-fill" :class="level" :style="{ width: `${value}%` }" />
    </div>
    <div v-if="$slots.default" class="meter-detail"><slot /></div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { CloseCircleFilled, WarningFilled } from '@ant-design/icons-vue'

/**
 * 使用率进度条：≥70% 偏高、≥90% 告警。状态同时用图标 + 文字表达，不只依赖颜色
 */
const props = defineProps({
  label: { type: String, required: true },
  percent: { type: Number, default: 0 },
  warning: { type: Number, default: 70 },
  critical: { type: Number, default: 90 }
})

const value = computed(() => Math.min(100, Math.max(0, props.percent || 0)))
const level = computed(() =>
  value.value >= props.critical ? 'critical' : value.value >= props.warning ? 'warning' : 'normal'
)
</script>

<style scoped>
.meter + .meter {
  margin-top: 16px;
}
.meter-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 6px;
}
.meter-label {
  color: var(--app-text-2);
}
.meter-value {
  font-weight: 600;
  color: var(--app-text);
  font-variant-numeric: tabular-nums;
}
.meter-state {
  margin-right: 6px;
  font-size: 12px;
  font-weight: 500;
  color: var(--app-text-2);
}
.meter-state.warning :deep(.anticon) {
  color: var(--status-warning);
}
.meter-state.critical :deep(.anticon) {
  color: var(--status-critical);
}
.meter-track {
  height: 8px;
  border-radius: 4px;
  background: var(--chart-track);
  overflow: hidden;
}
.meter-fill {
  height: 100%;
  border-radius: 4px;
  background: var(--chart-series-1);
  transition: width 0.4s ease;
}
.meter-fill.warning {
  background: var(--status-warning);
}
.meter-fill.critical {
  background: var(--status-critical);
}
.meter-detail {
  margin-top: 6px;
  font-size: 12px;
  color: var(--app-text-3);
  font-variant-numeric: tabular-nums;
}
</style>
