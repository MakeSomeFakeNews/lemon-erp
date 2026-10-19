<template>
  <view class="no-emp">
    <view class="no-emp__title">{{ t('hr.noEmployee') }}</view>
    <view class="no-emp__tip">{{ t('hr.noEmployeeTip') }}</view>
  </view>
</template>

<script setup>
import { useI18n } from '@/locales'

/** 当前账号没有关联员工档案时的提示（打卡、考勤、请假、工资条都需要关联） */
const { t } = useI18n()
</script>

<style lang="scss">
.no-emp {
  margin: 48rpx 24rpx;
  padding: 48rpx 36rpx;
  background: #fff;
  border-radius: $l-radius;
  text-align: center;
  &__title {
    font-size: 32rpx;
    font-weight: 600;
  }
  &__tip {
    margin-top: 20rpx;
    font-size: 26rpx;
    line-height: 1.7;
    color: $l-text-3;
  }
}
</style>
