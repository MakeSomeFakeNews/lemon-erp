<template>
  <view class="l-scan-input">
    <input
      v-model="text"
      class="l-scan-input__input"
      :placeholder="placeholder || t('common.scanPlaceholder')"
      :focus="focus"
      confirm-type="search"
      @confirm="submit"
    />
    <view class="l-scan-input__btn" @click="camera">{{ t('common.scan') }}</view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { useI18n } from '@/locales'
import { scanCode } from '@/composables/useAction'

/**
 * 扫码输入：点「扫码」调用摄像头；PDA 扫码枪按键盘输入加回车（confirm）。扫描结果通过 scan 事件返回，输入框随即清空
 */
defineProps({ placeholder: String, focus: Boolean })
const emit = defineEmits(['scan'])
const { t } = useI18n()
const text = ref('')

function submit() {
  const code = text.value.trim()
  text.value = ''
  if (code) emit('scan', code)
}

async function camera() {
  const code = await scanCode()
  if (code) emit('scan', code)
}
</script>

<style lang="scss">
.l-scan-input {
  display: flex;
  align-items: center;
  margin: 24rpx;
  padding: 0 0 0 24rpx;
  background: #fff;
  border-radius: $l-radius;
  overflow: hidden;
  &__input {
    flex: 1;
    height: 88rpx;
    font-size: 28rpx;
  }
  &__btn {
    padding: 0 32rpx;
    line-height: 88rpx;
    font-size: 28rpx;
    color: #fff;
    background: $l-primary;
  }
}
</style>
