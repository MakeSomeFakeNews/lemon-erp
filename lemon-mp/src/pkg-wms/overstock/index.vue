<template>
  <view>
    <view class="l-search">
      <input v-model="keyword" :placeholder="t('wms.overstock.keywordPh')" placeholder-class="l-placeholder" confirm-type="search" @confirm="reload" />
    </view>
    <l-segmented v-model="base" :options="baseOptions" @change="reload" />
    <view class="m-card os__filters">
      <picker :range="warehouseRange" range-key="name" :value="warehouseIndex" @change="changeWarehouse">
        <view class="m-input">
          <text class="m-input__label">{{ t('wms.common.warehouse') }}</text>
          <text class="m-input__value">{{ warehouseRange[warehouseIndex]?.name }}</text>
        </view>
      </picker>
      <view class="m-row">
        <picker mode="date" :value="date" :end="todayStr" class="os__half" @change="(e) => changeDate(e.detail.value)">
          <view class="m-input">
            <text class="m-input__label">{{ t('wms.overstock.date') }}</text>
            <text class="m-input__value">{{ date }}</text>
          </view>
        </picker>
        <view v-if="base === OverstockBase.RECENT_DAYS" class="m-input os__half">
          <text class="m-input__label">{{ t('wms.overstock.days') }}</text>
          <input v-model="days" type="number" class="m-input__value" @confirm="reload" @blur="reload" />
        </view>
      </view>
      <view class="m-sub os__tip">{{ tip }}</view>
    </view>

    <view v-if="summary && paged.list.length" class="os__summary">
      <text>{{ t('wms.overstock.summary', { n: summary.materialCount, qty: qty(summary.overQty) }) }}</text>
      <text v-if="costVisible">{{ t('wms.overstock.amount') }} {{ money(summary.amount) }}</text>
    </view>

    <view v-for="r in paged.list" :key="r.materialId" class="m-card" @click="openHistory(r)">
      <view class="m-row">
        <text class="m-title">{{ r.code }} {{ r.name }}</text>
        <l-tag v-if="r.ratio != null" :text="t('wms.overstock.ratio', { v: Number(r.ratio).toFixed(2) })" color="orange" />
      </view>
      <view v-if="r.spec" class="m-sub">{{ r.spec }}</view>
      <view class="os__nums">
        <view class="os__num">
          <text class="os__num-value">{{ qty(r.stockQty) }}</text>
          <text class="os__num-label">{{ t('wms.overstock.stock') }}{{ r.unit ? ` (${r.unit})` : '' }}</text>
        </view>
        <view class="os__num">
          <text class="os__num-value">{{ qty(r.baseQty) }}</text>
          <text class="os__num-label">{{ t('wms.overstock.base') }}</text>
        </view>
        <view class="os__num">
          <text class="os__num-value os__over">{{ qty(r.overQty) }}</text>
          <text class="os__num-label">{{ t('wms.overstock.over') }}</text>
        </view>
      </view>
      <view class="m-sub">
        {{ [t('wms.overstock.stockDays', { n: r.stockDays ?? '-' }), t('wms.overstock.lastPurchase', { date: r.lastPurchaseDate || t('wms.overstock.never') }),
          t('wms.overstock.lastSale', { date: r.lastSaleDate || t('wms.overstock.never') })].join(' · ') }}
      </view>
      <view v-if="costVisible" class="m-sub">{{ t('wms.overstock.amount') }} <text class="os__amount">{{ money(r.amount) }}</text></view>
    </view>
    <l-empty v-if="paged.loaded && !paged.list.length" />
    <l-load-more v-else-if="paged.list.length" :status="paged.status" />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { OverstockBase } from '@web/constants/enums'
import { pageOverstock, warehouseOptions } from '@/api/wms'
import { useTitle } from '@/composables/useTitle'
import { usePaged } from '@/composables/usePaged'
import { hasPerm } from '@/store/user'
import { money, qty, today } from '@/utils/format'

/**
 * 积压分析（issue #111，与 Web 端「进销存报表 → 积压分析」相同的接口）：截至某日库存超过积压基数（库存上限 / 上月销量 / 近 N 天销量）的商品，
 * 按资金占用（有「查看成本金额」权限时）或积压数量排序；有库存查询权限时点击进入经营历史
 */
const { t } = useI18n()
useTitle('wms.entry.overstock')
const todayStr = today()
const keyword = ref('')
const base = ref(OverstockBase.RECENT_DAYS)
const days = ref('60')
const date = ref(todayStr)
const warehouseId = ref(null)
const warehouses = ref([])
const summary = ref(null)
const result = ref({})
const costVisible = computed(() => !!result.value.costVisible)
const canHistory = hasPerm('wms:stock:list')

const baseOptions = computed(() => [
  { value: OverstockBase.RECENT_DAYS, label: t('wms.overstock.baseRecent', { n: days.value || '-' }) },
  { value: OverstockBase.LAST_MONTH, label: t('wms.overstock.baseLastMonth') },
  { value: OverstockBase.MAX_STOCK, label: t('wms.overstock.baseMax') }
])
const warehouseRange = computed(() => [{ id: null, name: t('wms.overstock.allWarehouses') }, ...warehouses.value])
const warehouseIndex = computed(() => Math.max(0, warehouseRange.value.findIndex((w) => w.id === warehouseId.value)))
const tip = computed(() =>
  result.value.salesBegin ? t('wms.overstock.tipSales', { begin: result.value.salesBegin, end: result.value.salesEnd }) : base.value === OverstockBase.MAX_STOCK ? t('wms.overstock.tipMax') : ''
)

const paged = usePaged(async (p) => {
  const n = Number(days.value)
  const res = await pageOverstock({
    ...p,
    keyword: keyword.value.trim() || undefined,
    warehouseId: warehouseId.value || undefined,
    date: date.value,
    base: base.value,
    days: base.value === OverstockBase.RECENT_DAYS ? n : undefined
  })
  if (p.current === 1) {
    summary.value = res.summary
    result.value = res
  }
  return res
})

onLoad(() => {
  warehouseOptions().then((list) => (warehouses.value = list || [])).catch(() => {})
  paged.refresh()
})
onPullDownRefresh(() => paged.refresh())
onReachBottom(() => paged.more())

function reload() {
  const n = Number(days.value)
  if (base.value === OverstockBase.RECENT_DAYS && (!Number.isInteger(n) || n < 1 || n > 366)) {
    uni.showToast({ title: t('wms.overstock.daysInvalid'), icon: 'none' })
    return
  }
  paged.refresh()
}

function changeWarehouse(e) {
  warehouseId.value = warehouseRange.value[e.detail.value]?.id ?? null
  reload()
}

function changeDate(v) {
  date.value = v
  reload()
}

function openHistory(r) {
  if (!canHistory) return
  const name = encodeURIComponent(`${r.code} ${r.name}`)
  uni.navigateTo({ url: `/pkg-wms/stock/history?materialId=${r.materialId}&name=${name}` })
}
</script>

<style lang="scss">
@import '../mobile.scss';

.os {
  &__filters {
    padding-top: 4rpx;
  }
  &__half {
    flex: 1;
    &:first-child {
      margin-right: 16rpx;
    }
  }
  &__tip {
    margin-top: 12rpx;
  }
  &__summary {
    display: flex;
    justify-content: space-between;
    margin: 0 24rpx 8rpx;
    font-size: 26rpx;
    color: $l-text-2;
  }
  &__nums {
    display: flex;
    margin: 20rpx 0 8rpx;
  }
  &__num {
    flex: 1;
    display: flex;
    flex-direction: column;
  }
  &__num-value {
    font-size: 34rpx;
    font-weight: 600;
  }
  &__over {
    color: #fa8c16;
  }
  &__num-label {
    margin-top: 4rpx;
    font-size: 22rpx;
    color: $l-text-3;
  }
  &__amount {
    font-weight: 600;
    color: $l-text;
  }
}
</style>
