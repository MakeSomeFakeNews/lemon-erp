<template>
  <view class="l-empty">
    <text class="l-empty__icon">○</text>
    <text class="l-empty__text">{{ text || t('common.empty') }}</text>
  </view>
</template>

<script setup>
import { useI18n } from '@/locales'

defineProps({ text: String })
const { t } = useI18n()
</script>

<style lang="scss">
.l-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 96rpx 0;
  color: $l-text-3;
  &__icon {
    font-size: 72rpx;
    line-height: 1;
    margin-bottom: 16rpx;
    color: #d9d9d9;
  }
  &__text {
    font-size: 26rpx;
  }
}
</style>
