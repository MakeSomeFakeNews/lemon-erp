<template>
  <view class="l-field">
    <view class="l-field__label">
      <text v-if="required" class="l-field__required">*</text>
      <text>{{ label }}</text>
    </view>
    <view class="l-field__control"><slot /></view>
  </view>
</template>

<script setup>
/** 表单项：标签 + 控件（input / picker / textarea 放在默认插槽中，控件加 class="l-input"） */
defineProps({ label: String, required: Boolean })
</script>

<style lang="scss">
.l-field {
  padding: 20rpx 0;
  border-bottom: 1rpx solid $l-border;
  &:last-child {
    border-bottom: none;
  }
  &__label {
    font-size: 26rpx;
    color: $l-text-2;
    margin-bottom: 12rpx;
  }
  &__required {
    color: $l-danger;
    margin-right: 4rpx;
  }
  &__control {
    font-size: 28rpx;
    color: $l-text;
  }
}
</style>
