<template>
  <view v-if="k">
    <view class="td__head">
      <view class="td__title">{{ k.title }}</view>
      <view class="td__tags">
        <l-tag :text="dictText(dict.crm_ticket_status, k.status)" :color="dictColor(dict.crm_ticket_status, k.status)" />
        <l-tag :text="dictText(dict.crm_ticket_priority, k.priority)" :color="dictColor(dict.crm_ticket_priority, k.priority)" />
        <text v-if="k.overdue" class="td__overdue">{{ t('crm.ticket.overdue') }}</text>
      </view>
    </view>

    <l-card :title="t('crm.ticket.base')">
      <l-cell :label="t('crm.ticket.no')" :value="k.ticketNo" />
      <l-cell :label="t('crm.ticket.customer')" :value="k.customerName" :arrow="canCustomer" @click="openCustomer" />
      <l-cell :label="t('crm.ticket.contact')">
        <text v-if="k.contactPhone" class="td__phone" @click="call">{{ [k.contactName, k.contactPhone].filter(Boolean).join(' ') }}</text>
        <text v-else>{{ k.contactName || '-' }}</text>
      </l-cell>
      <l-cell :label="t('crm.ticket.type')" :value="dictText(dict.crm_ticket_type, k.type)" />
      <l-cell :label="t('crm.ticket.channel')" :value="dictText(dict.crm_ticket_channel, k.channel)" />
      <l-cell :label="t('crm.ticket.product')" :value="[k.materialCode, k.materialName].filter(Boolean).join(' ')" />
      <l-cell :label="t('crm.ticket.serialNo')" :value="k.serialNo" />
      <l-cell :label="t('crm.ticket.assignee')" :value="k.assigneeName || t('crm.ticket.unassigned')" />
      <l-cell :label="t('crm.ticket.acceptTime')" :value="shortTime(k.createTime)" />
      <l-cell :label="t('crm.ticket.dueTime')" :value="shortTime(k.dueTime)" />
      <l-cell v-if="k.resolvedTime" :label="t('crm.ticket.resolvedTime')" :value="shortTime(k.resolvedTime)" />
      <l-cell v-if="k.satisfaction" :label="t('crm.ticket.satisfaction')" :value="'★'.repeat(k.satisfaction)" />
    </l-card>

    <l-card v-if="k.description" :title="t('crm.ticket.description')">
      <view class="td__text">{{ k.description }}</view>
    </l-card>
    <l-card v-if="k.solution" :title="t('crm.ticket.solution')">
      <view class="td__text">{{ k.solution }}</view>
    </l-card>

    <l-card :title="t('crm.ticket.logs')">
      <view v-if="!logs.length" class="l-muted td__none">{{ t('common.empty') }}</view>
      <view v-for="l in logs" :key="l.id" class="td__log">
        <view class="td__dot" />
        <view class="td__log-body">
          <view class="td__log-head">
            <text class="td__log-action">{{ t('crm.ticket.action.' + l.action) }}</text>
            <text class="l-muted td__meta">{{ shortTime(l.createTime) }} · {{ l.userName }}</text>
          </view>
          <view v-if="l.content" class="td__text">{{ l.content }}</view>
        </view>
      </view>
    </l-card>

    <template v-if="buttons.length">
      <view class="l-footer-space" />
      <view class="l-footer">
        <button v-for="b in buttons" :key="b" class="l-btn" :class="{ 'l-btn--ghost': b !== primary }" @click="act(b)">
          {{ t('crm.ticket.do.' + b) }}
        </button>
      </view>
    </template>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { TicketStatus } from '@web/constants/enums'
import { getTicket, ticketLogs } from '@/api/crm'
import { useTitle } from '@/composables/useTitle'
import { dictColor, dictText, useDict } from '@/store/dict'
import { hasPerm } from '@/store/user'
import { shortTime } from '@/utils/format'

const { t } = useI18n()
useTitle('crm.ticket.detailTitle')
const dict = useDict('crm_ticket_status', 'crm_ticket_priority', 'crm_ticket_type', 'crm_ticket_channel')
const id = ref(null)
const k = ref(null)
const logs = ref([])
const canCustomer = computed(() => hasPerm('crm:customer:list'))
let loaded = false

/** 待处理、处理中：添加处理记录、解决；已解决：关闭（满意度）、重新打开；已关闭：重新打开。指派在电脑端 */
const buttons = computed(() => {
  if (!k.value || !hasPerm('crm:ticket:handle')) return []
  const s = k.value.status
  if (s === TicketStatus.OPEN || s === TicketStatus.PROCESSING) return ['process', 'resolve']
  if (s === TicketStatus.RESOLVED) return ['reopen', 'close']
  return ['reopen']
})
const primary = computed(() => buttons.value[buttons.value.length - 1])

onLoad((q) => {
  id.value = Number(q.id)
})
// 处理后返回时刷新
onShow(() => {
  if (id.value) load()
})

async function load() {
  if (!loaded) uni.showLoading({ mask: true })
  try {
    const [d, ls] = await Promise.all([getTicket(id.value), ticketLogs(id.value).catch(() => [])])
    k.value = d
    logs.value = ls || []
    loaded = true
  } catch {
    // 已提示
  } finally {
    uni.hideLoading()
  }
}

function act(action) {
  uni.navigateTo({ url: `/pkg-crm/ticket/action?id=${id.value}&action=${action}` })
}

function call() {
  uni.makePhoneCall({ phoneNumber: k.value.contactPhone, fail: () => {} })
}

function openCustomer() {
  if (canCustomer.value && k.value.customerId) uni.navigateTo({ url: `/pkg-crm/customer/detail?customerId=${k.value.customerId}` })
}
</script>

<style lang="scss">
.td {
  &__head {
    padding: 32rpx 32rpx 8rpx;
  }
  &__title {
    font-size: 36rpx;
    font-weight: 600;
  }
  &__tags {
    display: flex;
    align-items: center;
    gap: 12rpx;
    margin-top: 12rpx;
  }
  &__overdue {
    font-size: 24rpx;
    color: $l-danger;
  }
  &__phone {
    color: $l-primary;
  }
  &__text {
    font-size: 26rpx;
    line-height: 1.6;
    color: $l-text-2;
    white-space: pre-wrap;
    word-break: break-all;
  }
  &__none {
    padding: 8rpx 0 16rpx;
    font-size: 26rpx;
  }
  &__log {
    display: flex;
    gap: 20rpx;
    padding: 12rpx 0;
  }
  &__dot {
    flex-shrink: 0;
    width: 16rpx;
    height: 16rpx;
    margin-top: 12rpx;
    border-radius: 50%;
    background: $l-primary;
  }
  &__log-body {
    flex: 1;
  }
  &__log-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16rpx;
  }
  &__log-action {
    font-size: 28rpx;
    font-weight: 600;
  }
  &__meta {
    font-size: 22rpx;
  }
}
</style>
