<template>
  <view>
    <view v-if="expired" class="pwd__tip">{{ t('common.password.expiredTip') }}</view>
    <l-card>
      <l-field :label="t('common.password.old')" required>
        <input v-model="form.oldPassword" class="l-input" password :placeholder="t('common.pleaseInput')" placeholder-class="l-placeholder" />
      </l-field>
      <l-field :label="t('common.password.new')" required>
        <input v-model="form.newPassword" class="l-input" password :placeholder="t('common.pleaseInput')" placeholder-class="l-placeholder" />
      </l-field>
      <l-field :label="t('common.password.confirm')" required>
        <input v-model="form.confirm" class="l-input" password :placeholder="t('common.pleaseInput')" placeholder-class="l-placeholder" />
      </l-field>
    </l-card>
    <view class="pwd__btn">
      <button class="l-btn" :loading="busy" :disabled="busy" @click="submit">{{ t('common.save') }}</button>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { changePassword } from '@/api/auth'
import { useTitle } from '@/composables/useTitle'

const { t } = useI18n()
useTitle('common.mine.password')
const expired = ref(false)
const busy = ref(false)
const form = reactive({ oldPassword: '', newPassword: '', confirm: '' })

onLoad((q) => {
  expired.value = q.expired === '1'
})

async function submit() {
  if (!form.oldPassword) return toast(t('common.required', { name: t('common.password.old') }))
  if (!form.newPassword) return toast(t('common.required', { name: t('common.password.new') }))
  if (form.newPassword !== form.confirm) return toast(t('common.password.mismatch'))
  busy.value = true
  try {
    await changePassword({ oldPassword: form.oldPassword, newPassword: form.newPassword })
  } catch {
    return
  } finally {
    busy.value = false
  }
  uni.showToast({ title: t('common.password.done'), icon: 'success' })
  // 登录时要求修改密码的，改完进入工作台
  setTimeout(() => (expired.value ? uni.switchTab({ url: '/pages/index/index' }) : uni.navigateBack()), 1000)
}

function toast(title) {
  uni.showToast({ title, icon: 'none' })
}
</script>

<style lang="scss">
.pwd {
  &__tip {
    margin: 24rpx 24rpx 0;
    padding: 20rpx 24rpx;
    border-radius: $l-radius;
    font-size: 24rpx;
    color: #d46b08;
    background: #fff7e6;
  }
  &__btn {
    margin: 48rpx 24rpx;
  }
}
</style>
