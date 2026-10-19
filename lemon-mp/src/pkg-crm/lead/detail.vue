<template>
  <view v-if="lead">
    <view class="ld__head">
      <view class="ld__name">{{ lead.company }}</view>
      <view class="ld__tags">
        <l-tag :text="dictText(dict.crm_lead_status, lead.status)" :color="dictColor(dict.crm_lead_status, lead.status)" />
        <text class="l-muted ld__no">{{ lead.leadNo }}</text>
      </view>
    </view>

    <l-card :title="t('crm.lead.base')">
      <l-cell :label="t('crm.lead.contactName')" :value="[lead.contactName, lead.title].filter(Boolean).join(' · ')" />
      <l-cell :label="t('crm.lead.phone')">
        <text v-if="lead.phone" class="ld__phone" @click="call">{{ lead.phone }}</text>
        <text v-else>-</text>
      </l-cell>
      <l-cell :label="t('crm.lead.email')" :value="lead.email" />
      <l-cell :label="t('crm.lead.source')" :value="dictText(dict.crm_source, lead.source)" />
      <l-cell :label="t('crm.lead.industry')" :value="dictText(dict.crm_industry, lead.industry)" />
      <l-cell :label="t('crm.lead.region')" :value="lead.region" />
      <l-cell :label="t('crm.lead.owner')" :value="lead.ownerName" />
      <l-cell v-if="lead.campaignName" :label="t('crm.lead.campaign')" :value="lead.campaignName" />
      <l-cell :label="t('crm.lead.requirement')" :value="lead.requirement" />
      <l-cell v-if="lead.status === LeadStatus.INVALID" :label="t('crm.lead.invalidReason')" :value="lead.invalidReason" />
      <l-cell
        v-if="lead.status === LeadStatus.CONVERTED && lead.customerId"
        :label="t('crm.lead.customer')"
        :value="lead.customerName"
        :arrow="canCustomer"
        @click="openCustomer"
      />
    </l-card>

    <l-card :title="t('crm.customer.followups')">
      <view v-if="!followups.length" class="l-muted ld__none">{{ t('common.empty') }}</view>
      <view v-for="f in followups" :key="f.id" class="ld__follow">
        <view class="ld__follow-head">
          <l-tag :text="dictText(dict.crm_follow_method, f.method)" :color="dictColor(dict.crm_follow_method, f.method)" />
          <text class="l-muted ld__meta">{{ shortTime(f.followTime) }} · {{ f.createByName }}</text>
        </view>
        <view class="ld__content">{{ f.content }}</view>
        <FollowupPhotos :followup-id="f.id" />
      </view>
    </l-card>

    <template v-if="actions.length">
      <view class="l-footer-space" />
      <view class="l-footer">
        <button v-if="canEditAny" class="l-btn l-btn--default ld__more" @click="more">{{ t('common.more') }}</button>
        <button v-if="canFollow" class="l-btn l-btn--ghost" @click="writeFollowup">{{ t('crm.followup.editTitle') }}</button>
        <button v-if="canConvert" class="l-btn" @click="convert">{{ t('crm.lead.convert') }}</button>
      </view>
    </template>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { FollowupTarget, LeadStatus } from '@web/constants/enums'
import { getLead, invalidLead, pageFollowups } from '@/api/crm'
import { runAction } from '@/composables/useAction'
import { useTitle } from '@/composables/useTitle'
import { dictColor, dictText, useDict } from '@/store/dict'
import { hasPerm } from '@/store/user'
import { shortTime } from '@/utils/format'
import FollowupPhotos from '../components/FollowupPhotos.vue'

const { t } = useI18n()
useTitle('crm.lead.detailTitle')
const dict = useDict('crm_lead_status', 'crm_source', 'crm_industry', 'crm_follow_method')
const id = ref(null)
const lead = ref(null)
const followups = ref([])
let loaded = false

/** 待跟进、跟进中的线索可以跟进、转化、标记无效 */
const isOpen = computed(() => lead.value && (lead.value.status === LeadStatus.NEW || lead.value.status === LeadStatus.FOLLOWING))
const canFollow = computed(() => isOpen.value && hasPerm('crm:followup:add'))
const canConvert = computed(() => isOpen.value && hasPerm('crm:lead:convert'))
const canEditAny = computed(() => isOpen.value && hasPerm('crm:lead:edit'))
const canCustomer = hasPerm('crm:customer:list')
const actions = computed(() => [canFollow.value, canConvert.value, canEditAny.value].filter(Boolean))

onLoad((q) => {
  id.value = Number(q.id)
})
// 跟进、编辑、转化返回时刷新
onShow(() => {
  if (id.value) load()
})

async function load() {
  if (!loaded) uni.showLoading({ mask: true })
  try {
    const [l, fs] = await Promise.all([
      getLead(id.value),
      pageFollowups({ current: 1, size: 20, targetType: FollowupTarget.LEAD, targetId: id.value }).catch(() => null)
    ])
    lead.value = l
    followups.value = fs?.records || []
    loaded = true
  } catch {
    // 已提示
  } finally {
    uni.hideLoading()
  }
}

function call() {
  uni.makePhoneCall({ phoneNumber: lead.value.phone, fail: () => {} })
}

function openCustomer() {
  if (canCustomer) uni.navigateTo({ url: `/pkg-crm/customer/detail?customerId=${lead.value.customerId}` })
}

function writeFollowup() {
  const l = lead.value
  uni.navigateTo({ url: `/pkg-crm/followup/edit?targetType=${FollowupTarget.LEAD}&targetId=${l.id}&name=${encodeURIComponent(l.company || '')}` })
}

function convert() {
  uni.navigateTo({ url: `/pkg-crm/lead/convert?id=${lead.value.id}&company=${encodeURIComponent(lead.value.company || '')}` })
}

function more() {
  const items = [
    { label: t('common.edit'), run: () => uni.navigateTo({ url: `/pkg-crm/lead/edit?id=${lead.value.id}` }) },
    { label: t('crm.lead.markInvalid'), run: markInvalid }
  ]
  uni.showActionSheet({ itemList: items.map((i) => i.label), success: (r) => items[r.tapIndex].run() })
}

function markInvalid() {
  uni.showModal({
    title: t('crm.lead.markInvalid'),
    editable: true,
    placeholderText: t('crm.lead.invalidPh'),
    confirmText: t('common.ok'),
    cancelText: t('common.cancel'),
    success: (r) => {
      if (!r.confirm) return
      runAction(() => invalidLead(id.value, (r.content || '').trim() || null), { success: t('crm.lead.invalidated'), then: load })
    }
  })
}
</script>

<style lang="scss">
.ld {
  &__head {
    padding: 32rpx 32rpx 8rpx;
  }
  &__name {
    font-size: 38rpx;
    font-weight: 600;
  }
  &__tags {
    display: flex;
    align-items: center;
    gap: 16rpx;
    margin-top: 12rpx;
  }
  &__no {
    font-size: 24rpx;
  }
  &__phone {
    color: $l-primary;
  }
  &__none {
    padding: 8rpx 0 16rpx;
    font-size: 26rpx;
  }
  &__follow {
    padding: 20rpx 0;
    border-bottom: 1rpx solid $l-border;
    &:last-child {
      border-bottom: none;
    }
  }
  &__follow-head {
    display: flex;
    align-items: center;
    gap: 16rpx;
  }
  &__meta {
    font-size: 24rpx;
  }
  &__content {
    margin-top: 10rpx;
    font-size: 26rpx;
    color: $l-text-2;
    line-height: 1.6;
    white-space: pre-wrap;
  }
  &__more {
    flex: 0 0 160rpx !important;
  }
}
</style>
