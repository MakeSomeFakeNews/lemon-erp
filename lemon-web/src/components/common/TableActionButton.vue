<template>
  <a-popconfirm
    v-if="confirmProps && !action.disabled"
    :title="confirmProps.title"
    :description="confirmProps.description"
    :ok-type="confirmProps.okType || (action.danger ? 'danger' : 'primary')"
    @confirm="action.onClick?.()"
  >
    <a-tooltip :title="tooltip">
      <a-button type="link" size="small" class="table-action-btn" :danger="action.danger" :loading="action.loading" :aria-label="action.label">
        <template v-if="action.icon" #icon><component :is="action.icon" /></template>
        <template v-if="text || !action.icon">{{ action.label }}</template>
      </a-button>
    </a-tooltip>
  </a-popconfirm>
  <a-tooltip v-else :title="tooltip">
    <a-button
      type="link"
      size="small"
      class="table-action-btn"
      :danger="action.danger"
      :disabled="action.disabled"
      :loading="action.loading"
      :aria-label="action.label"
      @click="action.onClick?.()"
    >
      <template v-if="action.icon" #icon><component :is="action.icon" /></template>
      <template v-if="text || !action.icon">{{ action.label }}</template>
    </a-button>
  </a-tooltip>
</template>

<script setup>
/**
 * 操作列中的一个按钮（TableActions 使用；自定义插槽中也用它渲染，保持样式一致）：
 * 只显示图标时悬停提示文字；text 为 true 时显示「图标 + 文字」；有 confirm 时点击先气泡确认
 */
import { computed } from 'vue'

const props = defineProps({
  action: { type: Object, required: true },
  text: { type: Boolean, default: false }
})

const confirmProps = computed(() => {
  const c = props.action.confirm
  if (!c) return null
  return typeof c === 'string' ? { title: c } : c
})

const tooltip = computed(() => {
  const label = props.text || !props.action.icon ? '' : props.action.label
  const lines = [label, props.action.tip].filter(Boolean)
  return lines.length ? lines.join(' · ') : undefined
})
</script>
