<template>
  <view>
    <l-card>
      <l-field v-if="action === 'close'" :label="t('crm.ticket.satisfaction')">
        <view class="ta__stars">
          <text v-for="n in 5" :key="n" class="ta__star" :class="{ 'ta__star--on': n <= satisfaction }" @click="satisfaction = n">★</text>
        </view>
      </l-field>
      <l-field :label="t('crm.ticket.content.' + action)" :required="action !== 'close'">
        <textarea v-model="content" class="l-textarea ta__textarea" maxlength="2000" :placeholder="t('crm.ticket.contentPh.' + action)" placeholder-class="l-placeholder" />
      </l-field>
    </l-card>
    <view class="l-footer-space" />
    <view class="l-footer">
      <button class="l-btn" :loading="busy" :disabled="busy" @click="submit">{{ t('common.submit') }}</button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { ticketAction } from '@/api/crm'

const { t } = useI18n()
const ACTIONS = ['process', 'resolve', 'close', 'reopen']
const id = ref(null)
const action = ref('process')
const content = ref('')
const satisfaction = ref(5)
const busy = ref(false)

onLoad((q) => {
  id.value = Number(q.id)
  action.value = ACTIONS.includes(q.action) ? q.action : 'process'
  uni.setNavigationBarTitle({ title: t('crm.ticket.do.' + action.value) })
})

async function submit() {
  const text = content.value.trim()
  if (action.value !== 'close' && !text) {
    return uni.showToast({ title: t('common.required', { name: t('crm.ticket.content.' + action.value) }), icon: 'none' })
  }
  busy.value = true
  try {
    await ticketAction(id.value, action.value, { content: text || null, satisfaction: action.value === 'close' ? satisfaction.value : null })
  } catch {
    return
  } finally {
    busy.value = false
  }
  uni.showToast({ title: t('common.success'), icon: 'success' })
  setTimeout(() => uni.navigateBack(), 800)
}
</script>

<style lang="scss">
.ta {
  &__textarea {
    height: 300rpx;
  }
  &__stars {
    display: flex;
    gap: 16rpx;
  }
  &__star {
    font-size: 56rpx;
    color: #e8e8e8;
    &--on {
      color: #fadb14;
    }
  }
}
</style>
