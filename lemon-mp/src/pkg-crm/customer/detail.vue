<template>
  <view v-if="c">
    <view class="cd__head">
      <view class="cd__name">{{ c.name }}</view>
      <view class="cd__tags">
        <l-tag v-if="c.stage" :text="dictText(dict.crm_customer_stage, c.stage)" :color="dictColor(dict.crm_customer_stage, c.stage)" />
        <l-tag v-if="c.level" :text="dictText(dict.scm_customer_level, c.level)" :color="dictColor(dict.scm_customer_level, c.level)" />
      </view>
    </view>

    <view v-if="summary" class="cd__stats">
      <view class="cd__stat">
        <view class="cd__stat-value">{{ shortMoney(summary.salesAmount) }}</view>
        <view class="cd__stat-label">{{ t('crm.customer.salesAmount') }}</view>
      </view>
      <view class="cd__stat">
        <view class="cd__stat-value">{{ summary.orderCount ?? 0 }}</view>
        <view class="cd__stat-label">{{ t('crm.customer.orderCount') }}</view>
      </view>
      <view class="cd__stat">
        <view class="cd__stat-value">{{ shortMoney(summary.arBalance) }}</view>
        <view class="cd__stat-label">{{ t('crm.customer.arBalance') }}</view>
      </view>
      <view class="cd__stat">
        <view class="cd__stat-value">{{ shortMoney(summary.wonAmount) }}</view>
        <view class="cd__stat-label">{{ t('crm.customer.wonAmount') }}</view>
      </view>
    </view>

    <l-card :title="t('crm.customer.base')">
      <l-cell :label="t('crm.customer.code')" :value="c.code" />
      <l-cell :label="t('crm.customer.owner')" :value="c.ownerName" />
      <l-cell :label="t('crm.customer.industry')" :value="dictText(dict.crm_industry, c.industry)" />
      <l-cell :label="t('crm.customer.region')" :value="c.region" />
      <l-cell :label="t('crm.customer.address')" :value="c.address" />
      <l-cell :label="t('crm.customer.lastFollow')" :value="shortTime(c.lastFollowTime)" />
      <l-cell :label="t('crm.customer.nextFollow')" :value="shortTime(c.nextFollowTime)" />
      <l-cell v-if="c.creditLimit" :label="t('crm.customer.creditLimit')" :value="money(c.creditLimit)" />
      <l-cell v-if="c.paymentDays" :label="t('crm.customer.paymentDays')" :value="c.paymentDays" />
      <l-cell v-if="canOrders" :label="t('crm.entry.order')" :value="t('crm.customer.viewAll')" arrow @click="openOrders" />
    </l-card>

    <l-card :title="t('crm.customer.contacts')">
      <view v-if="!contacts.length" class="l-muted cd__none">{{ t('common.empty') }}</view>
      <view v-for="ct in contacts" :key="ct.id" class="cd__contact">
        <view class="cd__contact-main">
          <view>
            <text class="cd__contact-name">{{ ct.name }}</text>
            <text v-if="ct.title" class="l-muted cd__contact-title">{{ ct.title }}</text>
            <l-tag v-if="ct.isPrimary === 1" :text="t('crm.customer.primary')" color="blue" />
          </view>
          <view class="l-muted cd__contact-phone">{{ [ct.phone, ct.wechat].filter(Boolean).join(' · ') }}</view>
        </view>
        <text v-if="ct.phone" class="cd__call" @click="call(ct.phone)">{{ t('crm.customer.call') }}</text>
      </view>
    </l-card>

    <l-card v-if="opportunities.length" :title="t('crm.customer.opportunities')">
      <view v-for="o in opportunities" :key="o.id" class="cd__row">
        <view class="cd__row-main">
          <text>{{ o.name }}</text>
          <view class="l-muted cd__row-sub">{{ money(o.amount) }} · {{ o.expectedDate || '-' }}</view>
        </view>
        <l-tag :text="dictText(dict.crm_opp_stage, o.stage)" :color="dictColor(dict.crm_opp_stage, o.stage)" />
      </view>
    </l-card>

    <l-card :title="t('crm.customer.followups')">
      <view v-if="!followups.length" class="l-muted cd__none">{{ t('common.empty') }}</view>
      <view v-for="f in followups" :key="f.id" class="cd__follow">
        <view class="cd__follow-head">
          <l-tag :text="dictText(dict.crm_follow_method, f.method)" :color="dictColor(dict.crm_follow_method, f.method)" />
          <text class="l-muted cd__follow-meta">{{ shortTime(f.followTime) }} · {{ f.createByName }}</text>
        </view>
        <view class="cd__follow-content">{{ f.content }}</view>
        <FollowupPhotos :followup-id="f.id" />
        <view v-if="f.targetType === FollowupTarget.OPPORTUNITY && f.targetName" class="l-muted cd__follow-meta">
          {{ t('crm.followup.targetType.OPPORTUNITY') }} · {{ f.targetName }}
        </view>
      </view>
    </l-card>

    <template v-if="canFollow">
      <view class="l-footer-space" />
      <view class="l-footer">
        <button class="l-btn" @click="writeFollowup">{{ t('crm.followup.editTitle') }}</button>
      </view>
    </template>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { FollowupTarget } from '@web/constants/enums'
import { customerSummary, getCustomer, listContacts, pageFollowups, pageOpportunities } from '@/api/crm'
import { useTitle } from '@/composables/useTitle'
import FollowupPhotos from '../components/FollowupPhotos.vue'
import { dictColor, dictText, useDict } from '@/store/dict'
import { ensureUser, hasPerm } from '@/store/user'
import { money, shortMoney, shortTime } from '@/utils/format'

const { t } = useI18n()
useTitle('crm.customer.detailTitle')
const dict = useDict('crm_customer_stage', 'scm_customer_level', 'crm_industry', 'crm_follow_method', 'crm_opp_stage')
const customerId = ref(null)
const c = ref(null)
const summary = ref(null)
const contacts = ref([])
const followups = ref([])
const opportunities = ref([])
const canFollow = computed(() => hasPerm('crm:followup:add'))
const canOrders = computed(() => hasPerm('scm:sales:list'))
let loaded = false

onLoad((q) => {
  customerId.value = Number(q.customerId)
})

// 写完跟进返回时刷新（最近跟进、下次跟进时间）
onShow(() => {
  if (customerId.value) load()
})

async function load() {
  await ensureUser()
  const id = customerId.value
  if (!loaded) uni.showLoading({ mask: true })
  try {
    const [detail, s, cts, fs, ops] = await Promise.all([
      getCustomer(id),
      customerSummary(id).catch(() => null),
      listContacts(id).catch(() => []),
      pageFollowups({ current: 1, size: 10, customerId: id }).catch(() => null),
      hasPerm('crm:opp:list') ? pageOpportunities({ current: 1, size: 20, customerId: id }).catch(() => null) : null
    ])
    c.value = detail
    summary.value = s
    contacts.value = cts || []
    followups.value = fs?.records || []
    opportunities.value = ops?.records || []
    loaded = true
  } catch {
    // 错误已提示
  } finally {
    uni.hideLoading()
  }
}

function call(phone) {
  uni.makePhoneCall({ phoneNumber: phone, fail: () => {} })
}

/** 销售订单的客户即 CRM 客户对应的往来单位（customerId） */
function openOrders() {
  uni.navigateTo({ url: `/pkg-crm/order/index?partnerId=${customerId.value}&name=${encodeURIComponent(c.value?.name || '')}` })
}

function writeFollowup() {
  uni.navigateTo({
    url: `/pkg-crm/followup/edit?targetType=${FollowupTarget.CUSTOMER}&targetId=${customerId.value}&name=${encodeURIComponent(c.value?.name || '')}`
  })
}
</script>

<style lang="scss">
.cd {
  &__head {
    padding: 32rpx 32rpx 8rpx;
  }
  &__name {
    font-size: 38rpx;
    font-weight: 600;
  }
  &__tags {
    display: flex;
    gap: 12rpx;
    margin-top: 12rpx;
  }
  &__stats {
    display: flex;
    margin: 24rpx 24rpx 0;
    padding: 24rpx 0;
    background: #fff;
    border-radius: $l-radius;
  }
  &__stat {
    flex: 1;
    text-align: center;
  }
  &__stat-value {
    font-size: 30rpx;
    font-weight: 600;
  }
  &__stat-label {
    margin-top: 6rpx;
    font-size: 22rpx;
    color: $l-text-3;
  }
  &__none {
    padding: 8rpx 0;
    font-size: 26rpx;
  }
  &__contact,
  &__row {
    display: flex;
    align-items: center;
    gap: 16rpx;
    padding: 18rpx 0;
    border-bottom: 1rpx solid $l-border;
    &:last-child {
      border-bottom: none;
    }
  }
  &__contact-main,
  &__row-main {
    flex: 1;
    min-width: 0;
  }
  &__contact-name {
    font-size: 28rpx;
    font-weight: 600;
    margin-right: 12rpx;
  }
  &__contact-title {
    font-size: 24rpx;
    margin-right: 12rpx;
  }
  &__contact-phone,
  &__row-sub {
    margin-top: 6rpx;
    font-size: 24rpx;
  }
  &__call {
    padding: 8rpx 24rpx;
    border: 1rpx solid $l-primary;
    border-radius: 28rpx;
    font-size: 24rpx;
    color: $l-primary;
  }
  &__follow {
    padding: 18rpx 0;
    border-bottom: 1rpx solid $l-border;
    &:last-child {
      border-bottom: none;
    }
  }
  &__follow-head {
    display: flex;
    align-items: center;
    gap: 12rpx;
  }
  &__follow-meta {
    font-size: 22rpx;
  }
  &__follow-content {
    margin: 10rpx 0 4rpx;
    font-size: 26rpx;
    color: $l-text-2;
    line-height: 1.6;
    white-space: pre-wrap;
  }
}
</style>
