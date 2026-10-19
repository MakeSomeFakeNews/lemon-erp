<template>
  <view class="l-cell" :class="{ 'l-cell--link': arrow }" hover-class="none" @click="emit('click')">
    <view v-if="icon" class="l-cell__icon" :style="{ background: iconColor }">
      <image class="l-cell__icon-img" :src="'/static/icons/' + icon + '.png'" />
    </view>
    <text class="l-cell__label" :class="{ 'l-cell__label--icon': icon }">{{ label }}</text>
    <view class="l-cell__value">
      <slot>{{ value === null || value === undefined || value === '' ? (arrow ? '' : '-') : value }}</slot>
    </view>
    <text v-if="arrow" class="l-cell__arrow">›</text>
  </view>
</template>

<script setup>
/**
 * 信息行：没有值时显示「-」；带箭头的是入口，没有值时不显示。
 * icon 为 static/icons 下的白色线条图标名，显示在 iconColor 色块上
 */
defineProps({ label: String, value: [String, Number], arrow: Boolean, icon: String, iconColor: { type: String, default: '#1677ff' } })
const emit = defineEmits(['click'])
</script>

<style lang="scss">
.l-cell {
  display: flex;
  align-items: flex-start;
  padding: 22rpx 0;
  font-size: 28rpx;
  border-bottom: 1rpx solid $l-border;
  &:last-child {
    border-bottom: none;
  }
  &__label {
    flex-shrink: 0;
    width: 200rpx;
    color: $l-text-3;
    &--icon {
      width: auto;
      margin-right: 24rpx;
      color: $l-text;
    }
  }
  &__icon {
    flex-shrink: 0;
    width: 52rpx;
    height: 52rpx;
    margin: -6rpx 20rpx -6rpx 0;
    border-radius: 14rpx;
    display: flex;
    align-items: center;
    justify-content: center;
  }
  &__icon-img {
    width: 32rpx;
    height: 32rpx;
  }
  &__value {
    flex: 1;
    color: $l-text;
    text-align: right;
    word-break: break-all;
  }
  &__arrow {
    margin-left: 12rpx;
    color: #bfbfbf;
    font-size: 32rpx;
    line-height: 1.2;
  }
}
</style>
