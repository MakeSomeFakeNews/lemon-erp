<template>
  <view>
    <!-- 条件：日期范围、单位编号 / 名称 / 电话 -->
    <view class="m-card">
      <view class="m-title">{{ name }}</view>
      <view class="lt-dates">
        <picker mode="date" :value="start" @change="(e) => change('start', e.detail.value)">
          <view class="lt-date" :class="{ 'l-placeholder': !start }">{{ start || t('lookup.trades.start') }}</view>
        </picker>
        <text class="l-muted">—</text>
        <picker mode="date" :value="end" @change="(e) => change('end', e.detail.value)">
          <view class="lt-date" :class="{ 'l-placeholder': !end }">{{ end || t('lookup.trades.end') }}</view>
        </picker>
        <text v-if="start || end" class="m-link" @click="clearDates">{{ t('wms.common.clear') }}</text>
      </view>
      <view class="l-search lt-search">
        <input v-model="keyword" :placeholder="t('lookup.trades.keywordPh')" placeholder-class="l-placeholder" confirm-type="search" @confirm="paged.refresh()" />
        <text class="m-link" @click="paged.refresh()">{{ t('common.search') }}</text>
      </view>
    </view>

    <!-- 明细：日期、数量、单价、单位名称、单位编号、制单、复核 -->
    <view v-for="(r, i) in paged.list" :key="`${r.docId}-${i}`" class="m-card">
      <view class="m-row">
        <text class="m-grow">{{ r.bizDate }}</text>
        <text class="lt-price">{{ price(r.price) }}</text>
      </view>
      <view class="m-row m-sub">
        <text class="m-grow">{{ r.partnerName }} · {{ r.partnerCode }}</text>
        <text class="m-nowrap">{{ t('lookup.trades.qty', { n: qty(r.qty) }) }}</text>
      </view>
      <view class="m-row m-sub">
        <text class="m-grow">{{ r.docNo }}</text>
        <text class="m-nowrap">{{ [r.createByName ? t('lookup.trades.prepared', { name: r.createByName }) : '', r.postByName ? t('lookup.trades.reviewed', { name: r.postByName }) : ''].filter(Boolean).join(' · ') }}</text>
      </view>
    </view>
    <l-empty v-if="paged.loaded && !paged.list.length" />
    <l-load-more v-if="paged.list.length" :status="paged.status" />
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { OrderKind } from '@web/constants/enums'
import { lookupTrades } from '@/api/lookup'
import { usePaged } from '@/composables/usePaged'
import { price, qty } from '@/utils/format'

/**
 * 更多交易历史（往来明细）：某商品的销售出库 / 采购入库明细，按日期范围、往来单位编号 / 名称 / 电话筛选，最近的在前。
 * 参数：materialId、kind（SALES / PURCHASE）、name（显示用）
 */
const { t } = useI18n()
const materialId = ref(null)
const kind = ref(OrderKind.SALES)
const name = ref('')
const start = ref('')
const end = ref('')
const keyword = ref('')

const paged = usePaged((p) =>
  lookupTrades(materialId.value, { ...p, kind: kind.value, keyword: keyword.value.trim() || undefined, start: start.value || undefined, end: end.value || undefined })
)

onLoad((q) => {
  materialId.value = Number(q.materialId)
  kind.value = q.kind === OrderKind.PURCHASE ? OrderKind.PURCHASE : OrderKind.SALES
  name.value = decodeURIComponent(q.name || '')
  paged.refresh()
})
// 标题按方向和界面语言设置（pages.json 中只能写一种）
onShow(() => uni.setNavigationBarTitle({ title: t(kind.value === OrderKind.PURCHASE ? 'lookup.trades.purchaseTitle' : 'lookup.trades.salesTitle') }))
onPullDownRefresh(() => paged.refresh())
onReachBottom(() => paged.more())

function change(field, value) {
  if (field === 'start') start.value = value
  else end.value = value
  paged.refresh()
}

function clearDates() {
  start.value = ''
  end.value = ''
  paged.refresh()
}
</script>

<style lang="scss">
@import '../mobile.scss';

.lt-dates {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-top: 16rpx;
}
.lt-date {
  padding: 12rpx 20rpx;
  font-size: 26rpx;
  background: $l-bg;
  border-radius: $l-radius;
}
.lt-search {
  display: flex;
  align-items: center;
  margin: 16rpx 0 0;
  input {
    flex: 1;
  }
}
.lt-price {
  font-size: 30rpx;
  font-weight: 600;
  color: $l-primary;
}
</style>
