<template>
  <view>
    <view class="lc__tip">{{ t('crm.lead.convertTip') }}</view>
    <l-card>
      <l-field :label="t('crm.lead.customerCode')">
        <input v-model="form.customerCode" class="l-input" maxlength="32" :placeholder="t('crm.lead.customerCodePh')" placeholder-class="l-placeholder" />
      </l-field>
      <l-field :label="t('crm.lead.createOpp')">
        <switch :checked="form.createOpportunity" color="#1677ff" @change="(e) => (form.createOpportunity = e.detail.value)" />
      </l-field>
      <template v-if="form.createOpportunity">
        <l-field :label="t('crm.lead.oppName')" required>
          <input v-model="form.opportunityName" class="l-input" maxlength="128" :placeholder="t('common.pleaseInput')" placeholder-class="l-placeholder" />
        </l-field>
        <l-field :label="t('crm.opportunity.amount')">
          <input v-model="form.amount" class="l-input" type="digit" :placeholder="t('common.pleaseInput')" placeholder-class="l-placeholder" />
        </l-field>
        <l-field :label="t('crm.opportunity.expectedDate')">
          <picker mode="date" :value="form.expectedDate" :start="today()" @change="(e) => (form.expectedDate = e.detail.value)">
            <view class="l-input" :class="{ 'l-placeholder': !form.expectedDate }">{{ form.expectedDate || t('common.pleaseSelect') }}</view>
          </picker>
        </l-field>
      </template>
    </l-card>
    <view class="l-footer-space" />
    <view class="l-footer">
      <button class="l-btn" :loading="busy" :disabled="busy" @click="submit">{{ t('crm.lead.convert') }}</button>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { convertLead } from '@/api/crm'
import { useTitle } from '@/composables/useTitle'
import { hasPerm } from '@/store/user'
import { today } from '@/utils/format'

const { t } = useI18n()
useTitle('crm.lead.convertTitle')
const id = ref(null)
const busy = ref(false)
const form = reactive({ customerCode: '', createOpportunity: true, opportunityName: '', amount: '', expectedDate: '' })

onLoad((q) => {
  id.value = Number(q.id)
  form.opportunityName = t('crm.lead.defaultOppName', { company: decodeURIComponent(q.company || '') })
})

async function submit() {
  const code = form.customerCode.trim()
  if (code && !/^[A-Za-z0-9_-]{1,32}$/.test(code)) return toast(t('crm.lead.customerCodeInvalid'))
  if (form.createOpportunity && !form.opportunityName.trim()) return toast(t('common.required', { name: t('crm.lead.oppName') }))
  const amount = form.amount === '' ? null : Number(form.amount)
  if (amount !== null && !(amount >= 0)) return toast(t('crm.lead.amountInvalid'))
  busy.value = true
  let res
  try {
    res = await convertLead(id.value, {
      customerCode: code || null,
      createOpportunity: form.createOpportunity,
      opportunityName: form.createOpportunity ? form.opportunityName.trim() : null,
      amount: form.createOpportunity ? amount : null,
      expectedDate: form.createOpportunity ? form.expectedDate || null : null
    })
  } catch {
    return
  } finally {
    busy.value = false
  }
  uni.showToast({ title: t('crm.lead.converted'), icon: 'success' })
  // 转化后进入新客户的详情（有客户权限时），否则回到线索
  setTimeout(() => {
    if (res?.customerId && hasPerm('crm:customer:list')) uni.redirectTo({ url: `/pkg-crm/customer/detail?customerId=${res.customerId}` })
    else uni.navigateBack()
  }, 800)
}

function toast(title) {
  uni.showToast({ title, icon: 'none' })
}
</script>

<style lang="scss">
.lc__tip {
  margin: 24rpx 24rpx 0;
  padding: 20rpx 24rpx;
  border-radius: $l-radius;
  font-size: 24rpx;
  line-height: 1.6;
  color: #0958d9;
  background: #e6f4ff;
}
</style>
