<template>
  <view class="l-segmented">
    <view
      v-for="o in options"
      :key="o.value"
      class="l-segmented__item"
      :class="{ 'l-segmented__item--active': o.value === modelValue }"
      @click="select(o.value)"
    >
      <text>{{ o.label }}</text>
      <text v-if="o.badge" class="l-segmented__badge">{{ o.badge > 99 ? '99+' : o.badge }}</text>
    </view>
  </view>
</template>

<script setup>
/** 顶部分段切换：options [{ label, value, badge? }] */
const props = defineProps({ modelValue: [String, Number], options: { type: Array, default: () => [] } })
const emit = defineEmits(['update:modelValue', 'change'])

function select(v) {
  if (v === props.modelValue) return
  emit('update:modelValue', v)
  emit('change', v)
}
</script>

<style lang="scss">
.l-segmented {
  display: flex;
  background: #fff;
  border-bottom: 1rpx solid $l-border;
  &__item {
    flex: 1;
    position: relative;
    padding: 24rpx 0;
    text-align: center;
    font-size: 28rpx;
    color: $l-text-2;
    &--active {
      color: $l-primary;
      font-weight: 600;
      &::after {
        content: '';
        position: absolute;
        left: 50%;
        bottom: 0;
        width: 48rpx;
        height: 6rpx;
        margin-left: -24rpx;
        border-radius: 3rpx;
        background: $l-primary;
      }
    }
  }
  &__badge {
    margin-left: 8rpx;
    padding: 0 10rpx;
    border-radius: 18rpx;
    font-size: 20rpx;
    line-height: 32rpx;
    color: #fff;
    background: $l-danger;
  }
}
</style>
