<template>
  <view>
    <l-segmented v-model="status" :options="statuses" @change="paged.refresh()" />
    <view v-if="partnerName" class="so__filter">
      <text>{{ t('crm.order.customer') }} · {{ partnerName }}</text>
    </view>
    <view class="l-search">
      <picker v-if="!partnerId" :range="searchFields" range-key="label" :value="searchIndex" @change="changeSearchBy">
        <view class="so__by">{{ searchFields[searchIndex].label }} ▾</view>
      </picker>
      <input
        v-model="keyword"
        :placeholder="searchBy === 'partner' ? t('crm.order.searchCustomerPh') : t('crm.order.searchPh')"
        placeholder-class="l-placeholder"
        confirm-type="search"
        @confirm="paged.refresh()"
      />
    </view>
    <view v-for="o in paged.list" :key="o.id" class="l-item" @click="open(o)">
      <view class="l-item__head">
        <text class="l-item__title">{{ o.orderNo }}</text>
        <l-tag :text="dictText(dict.scm_order_status, o.status)" :color="dictColor(dict.scm_order_status, o.status)" />
      </view>
      <view class="l-item__desc">{{ [o.partnerName, o.salesmanName].filter(Boolean).join(' · ') }}</view>
      <view class="so__row">
        <text class="so__amount">{{ money(o.totalAmount) }}</text>
        <text class="l-muted so__meta">{{ o.orderDate }}</text>
      </view>
      <view v-if="o.doneQty !== null && o.doneQty !== undefined && Number(o.totalQty) > 0" class="so__progress">
        <view class="so__bar"><view class="so__bar-inner" :style="{ width: percent(o) + '%' }" /></view>
        <text class="l-muted so__meta">{{ t('crm.order.shipped', { done: qty(o.doneQty), total: qty(o.totalQty) }) }}</text>
      </view>
    </view>
    <l-empty v-if="paged.loaded && !paged.list.length" />
    <l-load-more v-else-if="paged.list.length" :status="paged.status" />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { OrderStatus } from '@web/constants/enums'
import { pageSalesOrders } from '@/api/crm'
import { usePaged } from '@/composables/usePaged'
import { useTitle } from '@/composables/useTitle'
import { dictColor, dictText, useDict } from '@/store/dict'
import { money, qty } from '@/utils/format'

const { t } = useI18n()
useTitle('crm.order.title')
const dict = useDict('scm_order_status')
const status = ref('')
/** 搜索订单号或客户（名称、编码）；从客户详情进入时只按订单号搜索 */
const searchBy = ref('orderNo')
const keyword = ref('')
const searchFields = computed(() => [
  { label: t('crm.order.byOrderNo'), value: 'orderNo' },
  { label: t('crm.order.byCustomer'), value: 'partner' }
])
const searchIndex = computed(() => Math.max(0, searchFields.value.findIndex((f) => f.value === searchBy.value)))
const partnerId = ref(null)
const partnerName = ref('')
// 常看的状态：执行中（已审核、部分出库）与已完成；其余在全部中查看
const statuses = computed(() => [
  { label: t('common.all'), value: '' },
  ...[OrderStatus.APPROVED, OrderStatus.PARTIAL, OrderStatus.COMPLETED].map((s) => ({ label: dictText(dict.scm_order_status, s), value: s }))
])
const paged = usePaged((p) =>
  pageSalesOrders({
    ...p,
    status: status.value || undefined,
    orderNo: searchBy.value === 'orderNo' ? keyword.value.trim() : undefined,
    partnerName: searchBy.value === 'partner' ? keyword.value.trim() : undefined,
    partnerId: partnerId.value || undefined
  })
)

/** 从客户详情进入时只看该客户的订单 */
onLoad((q) => {
  if (q.partnerId) {
    partnerId.value = Number(q.partnerId)
    partnerName.value = decodeURIComponent(q.name || '')
  }
  paged.refresh()
})
onPullDownRefresh(() => paged.refresh())
onReachBottom(() => paged.more())

function changeSearchBy(e) {
  searchBy.value = searchFields.value[e.detail.value].value
  if (keyword.value.trim()) paged.refresh()
}

const percent = (o) => Math.min(100, Math.round((Number(o.doneQty) / Number(o.totalQty)) * 100))

function open(o) {
  uni.navigateTo({ url: `/pkg-crm/order/detail?id=${o.id}` })
}
</script>

<style lang="scss">
.so {
  &__by {
    padding-right: 20rpx;
    margin-right: 20rpx;
    border-right: 1rpx solid $l-border;
    font-size: 26rpx;
    color: $l-text-2;
    white-space: nowrap;
  }
  &__filter {
    margin: 20rpx 24rpx 0;
    font-size: 26rpx;
    color: $l-text-2;
  }
  &__row {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    margin-top: 12rpx;
  }
  &__amount {
    font-size: 32rpx;
    font-weight: 600;
  }
  &__meta {
    font-size: 24rpx;
  }
  &__progress {
    display: flex;
    align-items: center;
    gap: 16rpx;
    margin-top: 12rpx;
  }
  &__bar {
    flex: 1;
    height: 10rpx;
    border-radius: 5rpx;
    background: #f0f0f0;
    overflow: hidden;
  }
  &__bar-inner {
    height: 100%;
    background: $l-primary;
  }
}
</style>
