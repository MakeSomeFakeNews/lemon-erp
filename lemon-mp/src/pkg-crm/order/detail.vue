<template>
  <view v-if="o">
    <view class="od__head">
      <view class="od__no">{{ o.orderNo }}</view>
      <l-tag :text="dictText(dict.scm_order_status, o.status)" :color="dictColor(dict.scm_order_status, o.status)" />
    </view>

    <l-card>
      <l-cell :label="t('crm.order.customer')" :value="o.partnerName" />
      <l-cell :label="t('crm.order.orderDate')" :value="o.orderDate" />
      <l-cell :label="t('crm.order.deliveryDate')" :value="o.deliveryDate" />
      <l-cell :label="t('crm.order.salesman')" :value="o.salesmanName" />
      <l-cell :label="t('crm.order.warehouse')" :value="o.warehouseName" />
      <l-cell :label="t('crm.order.totalAmount')" :value="money(o.totalAmount)" />
      <l-cell :label="t('crm.order.taxAmount')" :value="money(o.taxAmount)" />
      <l-cell :label="t('crm.order.progress')" :value="t('crm.order.shipped', { done: qty(shippedQty), total: qty(o.totalQty) })" />
      <l-cell v-if="o.remark" :label="t('crm.order.remark')" :value="o.remark" />
    </l-card>

    <l-card :title="t('crm.order.items')">
      <template #extra>{{ (o.items || []).length }}</template>
      <view v-for="it in o.items || []" :key="it.id" class="od__line">
        <view class="od__line-head">
          <text class="od__line-name">{{ it.materialName }}</text>
          <l-tag v-if="it.gift === 1" :text="t('crm.order.gift')" color="orange" />
        </view>
        <view class="l-muted od__line-sub">{{ [it.materialCode, it.spec].filter(Boolean).join(' · ') }}</view>
        <view class="od__line-nums">
          <text>{{ t('crm.order.qty') }} {{ qty(it.qty) }} {{ it.unitName || it.unit || '' }}</text>
          <text>{{ t('crm.order.doneQty') }} {{ qty(it.doneQty) }}</text>
          <text v-if="Number(it.returnQty) > 0">{{ t('crm.order.returnQty') }} {{ qty(it.returnQty) }}</text>
        </view>
        <view class="od__line-nums">
          <text>{{ t('crm.order.price') }} {{ money(it.price) }}</text>
          <text class="od__line-amount">{{ money(it.amount) }}</text>
        </view>
      </view>
    </l-card>

    <l-card v-if="trace.tradeDocs?.length" :title="t('crm.order.outDocs')">
      <view v-for="d in trace.tradeDocs" :key="d.id" class="od__row">
        <view class="od__row-main">
          <text>{{ d.docNo }}</text>
          <view class="l-muted od__row-sub">{{ dictText(dict.scm_trade_kind, d.kind) }} · {{ d.bizDate }}</view>
        </view>
        <text>{{ money(d.totalAmount) }}</text>
      </view>
    </l-card>

    <l-card v-if="trace.bills?.length" :title="t('crm.order.bills')">
      <template #extra>{{ t('crm.order.billSummary', { total: money(billTotal), settled: money(billSettled) }) }}</template>
      <view v-for="b in trace.bills" :key="b.id" class="od__row">
        <view class="od__row-main">
          <text>{{ b.billNo }}</text>
          <view class="l-muted od__row-sub">{{ b.billDate }} · {{ t('crm.order.settled', { amount: money(b.settledAmount) }) }}</view>
        </view>
        <view class="od__row-end">
          <text>{{ money(b.amount) }}</text>
          <l-tag v-if="b.status" :text="dictText(dict.fin_bill_status, b.status)" :color="dictColor(dict.fin_bill_status, b.status)" />
        </view>
      </view>
    </l-card>

    <l-card v-if="trace.shipments?.length" :title="t('crm.order.shipments')">
      <view v-for="s in trace.shipments" :key="s.id" class="od__row">
        <view class="od__row-main">
          <text>{{ s.shipNo }}</text>
          <view class="l-muted od__row-sub">
            {{ [dictText(dict.scm_carrier, s.carrier), s.trackingNo && t('crm.order.trackingNo', { no: s.trackingNo }), s.shipDate].filter(Boolean).join(' · ') }}
          </view>
        </view>
        <l-tag v-if="s.status" :text="dictText(dict.scm_shipment_status, s.status)" :color="dictColor(dict.scm_shipment_status, s.status)" />
      </view>
    </l-card>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { getSalesOrder, salesOrderTrace } from '@/api/crm'
import { useTitle } from '@/composables/useTitle'
import { dictColor, dictText, useDict } from '@/store/dict'
import { money, qty } from '@/utils/format'

const { t } = useI18n()
useTitle('crm.order.detailTitle')
const dict = useDict('scm_order_status', 'scm_trade_kind', 'fin_bill_status', 'scm_carrier', 'scm_shipment_status')
const o = ref(null)
/** 订单追溯：出库单、应收单、发运（按当前用户的权限返回） */
const trace = ref({})

const shippedQty = computed(() => (o.value?.items || []).reduce((sum, it) => sum + (Number(it.doneQty) || 0), 0))
const billTotal = computed(() => (trace.value.bills || []).reduce((sum, b) => sum + (Number(b.amount) || 0), 0))
const billSettled = computed(() => (trace.value.bills || []).reduce((sum, b) => sum + (Number(b.settledAmount) || 0), 0))

onLoad(async (q) => {
  const id = Number(q.id)
  uni.showLoading({ mask: true })
  try {
    const [detail, tr] = await Promise.all([getSalesOrder(id), salesOrderTrace(id).catch(() => ({}))])
    o.value = detail
    trace.value = tr || {}
  } catch {
    // 错误已提示
  } finally {
    uni.hideLoading()
  }
})
</script>

<style lang="scss">
.od {
  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 32rpx 32rpx 0;
  }
  &__no {
    font-size: 36rpx;
    font-weight: 600;
  }
  &__line {
    padding: 18rpx 0;
    border-bottom: 1rpx solid $l-border;
    &:last-child {
      border-bottom: none;
    }
  }
  &__line-head {
    display: flex;
    align-items: center;
    gap: 12rpx;
  }
  &__line-name {
    font-size: 28rpx;
    font-weight: 600;
  }
  &__line-sub {
    margin-top: 4rpx;
    font-size: 22rpx;
  }
  &__line-nums {
    display: flex;
    gap: 28rpx;
    margin-top: 8rpx;
    font-size: 24rpx;
    color: $l-text-2;
  }
  &__line-amount {
    margin-left: auto;
    font-weight: 600;
    color: $l-text;
  }
  &__row {
    display: flex;
    align-items: center;
    gap: 16rpx;
    padding: 18rpx 0;
    font-size: 26rpx;
    border-bottom: 1rpx solid $l-border;
    &:last-child {
      border-bottom: none;
    }
  }
  &__row-main {
    flex: 1;
    min-width: 0;
  }
  &__row-sub {
    margin-top: 4rpx;
    font-size: 22rpx;
  }
  &__row-end {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 6rpx;
  }
}
</style>
