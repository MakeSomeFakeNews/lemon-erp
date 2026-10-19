<template>
  <view>
    <l-segmented v-model="status" :options="statuses" @change="paged.refresh()" />
    <view class="tk__bar">
      <view class="l-search tk__search">
        <input v-model="keyword" :placeholder="t('crm.ticket.searchPh')" placeholder-class="l-placeholder" confirm-type="search" @confirm="paged.refresh()" />
      </view>
      <view class="tk__mine">
        <text>{{ t('crm.ticket.onlyMine') }}</text>
        <switch :checked="mine" color="#1677ff" style="transform: scale(0.8)" @change="toggleMine" />
      </view>
    </view>
    <view v-for="k in paged.list" :key="k.id" class="l-item" @click="open(k)">
      <view class="l-item__head">
        <text class="l-item__title">{{ k.title }}</text>
        <l-tag :text="dictText(dict.crm_ticket_status, k.status)" :color="dictColor(dict.crm_ticket_status, k.status)" />
      </view>
      <view class="l-item__desc">{{ [k.ticketNo, k.customerName].filter(Boolean).join(' · ') }}</view>
      <view class="tk__row">
        <l-tag :text="dictText(dict.crm_ticket_priority, k.priority)" :color="dictColor(dict.crm_ticket_priority, k.priority)" />
        <l-tag v-if="k.type" :text="dictText(dict.crm_ticket_type, k.type)" />
        <text class="l-muted tk__meta">{{ k.assigneeName || t('crm.ticket.unassigned') }}</text>
        <text v-if="k.overdue" class="tk__overdue">{{ t('crm.ticket.overdue') }}</text>
        <text v-else-if="k.dueTime && isActive(k)" class="l-muted tk__meta">{{ t('crm.ticket.dueAt', { time: shortTime(k.dueTime) }) }}</text>
      </view>
    </view>
    <l-empty v-if="paged.loaded && !paged.list.length" />
    <l-load-more v-else-if="paged.list.length" :status="paged.status" />

    <template v-if="canAdd">
      <view class="l-footer-space" />
      <view class="l-footer">
        <button class="l-btn" @click="add">{{ t('crm.ticket.add') }}</button>
      </view>
    </template>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { TicketStatus } from '@web/constants/enums'
import { pageTickets } from '@/api/crm'
import { usePaged } from '@/composables/usePaged'
import { useTitle } from '@/composables/useTitle'
import { dictColor, dictText, useDict } from '@/store/dict'
import { hasPerm } from '@/store/user'
import { shortTime } from '@/utils/format'

const { t } = useI18n()
useTitle('crm.ticket.title')
const dict = useDict('crm_ticket_status', 'crm_ticket_priority', 'crm_ticket_type')
const canAdd = hasPerm('crm:ticket:add')
// 默认看待处理的；「只看我的」为处理人是我
const status = ref(TicketStatus.OPEN)
const statuses = computed(() => [
  ...[TicketStatus.OPEN, TicketStatus.PROCESSING, TicketStatus.RESOLVED].map((s) => ({ label: dictText(dict.crm_ticket_status, s), value: s })),
  { label: t('common.all'), value: '' }
])
const mine = ref(true)
const keyword = ref('')
const paged = usePaged((p) => pageTickets({ ...p, status: status.value || undefined, mine: mine.value, keyword: keyword.value.trim() }))
let shown = false

onLoad(() => paged.refresh())
onShow(() => {
  if (shown) paged.refresh()
  shown = true
})
onPullDownRefresh(() => paged.refresh())
onReachBottom(() => paged.more())

const isActive = (k) => k.status === TicketStatus.OPEN || k.status === TicketStatus.PROCESSING

function toggleMine(e) {
  mine.value = e.detail.value
  paged.refresh()
}

function open(k) {
  uni.navigateTo({ url: `/pkg-crm/ticket/detail?id=${k.id}` })
}

function add() {
  uni.navigateTo({ url: '/pkg-crm/ticket/edit' })
}
</script>

<style lang="scss">
.tk {
  &__bar {
    display: flex;
    align-items: center;
  }
  &__search {
    flex: 1;
    margin-right: 0 !important;
  }
  &__mine {
    display: flex;
    align-items: center;
    padding: 0 16rpx 0 20rpx;
    font-size: 24rpx;
    color: $l-text-2;
    white-space: nowrap;
  }
  &__row {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 12rpx;
    margin-top: 12rpx;
  }
  &__meta {
    font-size: 24rpx;
  }
  &__overdue {
    font-size: 24rpx;
    color: $l-danger;
  }
}
</style>
