<template>
  <view class="l-load-more">{{ status === 'loading' ? t('common.loading') : status === 'noMore' ? t('common.noMore') : t('common.pullUp') }}</view>
</template>

<script setup>
import { useI18n } from '@/locales'

defineProps({ status: { type: String, default: 'more' } })
const { t } = useI18n()
</script>

<style lang="scss">
.l-load-more {
  padding: 24rpx 0 40rpx;
  text-align: center;
  font-size: 24rpx;
  color: $l-text-3;
}
</style>
