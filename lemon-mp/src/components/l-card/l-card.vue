<template>
  <view class="l-card" @click="emit('click')">
    <view v-if="title || $slots.title || $slots.extra" class="l-card__head">
      <view class="l-card__title">
        <slot name="title">{{ title }}</slot>
      </view>
      <view class="l-card__extra"><slot name="extra" /></view>
    </view>
    <view class="l-card__body"><slot /></view>
  </view>
</template>

<script setup>
defineProps({ title: String })
const emit = defineEmits(['click'])
</script>

<style lang="scss">
.l-card {
  margin: 24rpx;
  padding: 24rpx 28rpx;
  background: #fff;
  border-radius: $l-radius;
  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-bottom: 16rpx;
  }
  &__title {
    font-size: 30rpx;
    font-weight: 600;
    color: $l-text;
  }
  &__extra {
    font-size: 24rpx;
    color: $l-text-3;
  }
}
</style>
