<template>
  <text class="l-tag" :class="'l-tag--' + tone">{{ text }}</text>
</template>

<script setup>
import { computed } from 'vue'

/** 状态标签。color 可用 Web 端字典的 tagColor（green / red / blue / orange / gold / cyan / purple…）或 success / danger / warning / primary */
const props = defineProps({ text: [String, Number], color: String })

const TONES = {
  green: 'success',
  success: 'success',
  cyan: 'success',
  red: 'danger',
  error: 'danger',
  danger: 'danger',
  volcano: 'danger',
  orange: 'warning',
  gold: 'warning',
  warning: 'warning',
  blue: 'primary',
  geekblue: 'primary',
  processing: 'primary',
  primary: 'primary',
  purple: 'purple'
}
const tone = computed(() => TONES[props.color] || 'default')
</script>

<style lang="scss">
.l-tag {
  display: inline-block;
  padding: 2rpx 14rpx;
  border-radius: 8rpx;
  font-size: 22rpx;
  line-height: 36rpx;
  white-space: nowrap;
  color: $l-text-2;
  background: #f5f5f5;
  &--success {
    color: #389e0d;
    background: #f6ffed;
  }
  &--danger {
    color: #cf1322;
    background: #fff1f0;
  }
  &--warning {
    color: #d46b08;
    background: #fff7e6;
  }
  &--primary {
    color: #0958d9;
    background: #e6f4ff;
  }
  &--purple {
    color: #531dab;
    background: #f9f0ff;
  }
}
</style>
