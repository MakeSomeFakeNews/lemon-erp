<template>
  <view>
    <view class="m-card">
      <view class="m-title">{{ name }}</view>
      <view class="l-search lr-search">
        <input v-model="keyword" :placeholder="t('lookup.ref.keywordPh')" placeholder-class="l-placeholder" confirm-type="search" @confirm="load" />
        <text class="m-link" @click="load">{{ t('common.search') }}</text>
      </view>
    </view>
    <l-segmented v-if="tabs.length" v-model="tab" :options="tabs" />

    <view v-if="loading" class="ss-loading l-muted">{{ t('common.loading') }}</view>
    <l-empty v-else-if="data && !tabs.length" :text="t('lookup.ref.noPermission')" />
    <block v-else-if="current">
      <!-- 报价 / 销售 / 进货：日期、单号、单位、数量、单价 -->
      <block v-if="!isCompare">
        <view v-for="(r, i) in current" :key="i" class="m-card">
          <view class="m-row">
            <text class="m-grow">{{ r.bizDate }} · {{ r.partnerName }}</text>
            <text class="lr-price">{{ price(r.price) }}</text>
          </view>
          <view class="m-row m-sub">
            <text class="m-grow">{{ r.docNo }}</text>
            <text class="m-nowrap">{{ t('lookup.trades.qty', { n: qty(r.qty) }) }}</text>
          </view>
        </view>
      </block>
      <!-- 客户 / 供货商对比：每个往来单位的最新、最低、最高价 -->
      <block v-else>
        <view v-for="(r, i) in current" :key="i" class="m-card">
          <view class="m-row">
            <text class="m-grow">{{ r.partnerName }}</text>
            <text class="m-nowrap m-sub">{{ r.partnerCode }}</text>
          </view>
          <view class="m-sub">{{ t('lookup.ref.last', { price: price(r.lastPrice), date: r.lastDate }) }}</view>
          <view class="m-sub">{{ t('lookup.ref.min', { price: price(r.minPrice), date: r.minDate }) }}</view>
          <view class="m-sub">{{ t('lookup.ref.max', { price: price(r.maxPrice), date: r.maxDate }) }}</view>
        </view>
      </block>
      <l-empty v-if="!current.length" />
    </block>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { OrderKind } from '@web/constants/enums'
import { lookupReference } from '@/api/lookup'
import { useTitle } from '@/composables/useTitle'
import { price, qty } from '@/utils/format'

/**
 * 更多内容：与 Web 端销售单明细上的「价格参考」相同的接口和标签页（报价、销售、进货、客户、供货商），
 * 后端按权限裁剪分区，没有权限的标签页不显示（只能看售价的人看不到进货、供货商）。参数：materialId、kind（默认打开的方向）、name
 */
const { t } = useI18n()
useTitle('lookup.ref.title')
const TABS = ['quotations', 'sales', 'purchases', 'customerCompare', 'supplierCompare']
const COMPARE = ['customerCompare', 'supplierCompare']

const materialId = ref(null)
const name = ref('')
const keyword = ref('')
const data = ref(null)
const loading = ref(false)
const tab = ref('')

const tabs = computed(() => TABS.filter((k) => Array.isArray(data.value?.[k])).map((k) => ({ value: k, label: t(`lookup.ref.${k}`) })))
const current = computed(() => data.value?.[tab.value] || null)
const isCompare = computed(() => COMPARE.includes(tab.value))

let initialTab = 'sales'
onLoad((q) => {
  materialId.value = Number(q.materialId)
  name.value = decodeURIComponent(q.name || '')
  initialTab = q.kind === OrderKind.PURCHASE ? 'purchases' : 'sales'
  load()
})

async function load() {
  loading.value = true
  try {
    data.value = (await lookupReference({ materialId: materialId.value, keyword: keyword.value.trim() || undefined }).catch(() => null)) || {}
    const keys = tabs.value.map((o) => o.value)
    if (!keys.includes(tab.value)) tab.value = keys.includes(initialTab) ? initialTab : keys[0] || ''
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss">
@import '../mobile.scss';
@import '../sheet.scss';

.lr-search {
  display: flex;
  align-items: center;
  margin: 16rpx 0 0;
  input {
    flex: 1;
  }
}
.lr-price {
  font-size: 30rpx;
  font-weight: 600;
  color: $l-primary;
}
</style>
