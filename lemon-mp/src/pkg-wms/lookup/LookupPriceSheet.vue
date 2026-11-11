<template>
  <view v-if="visible" class="ss-mask" @click.self="visible = false">
    <view class="ss-panel">
      <view class="ss-title">{{ material?.name }}{{ material?.spec ? ` ${material.spec}` : '' }}</view>
      <l-segmented v-if="kinds.length > 1" v-model="kind" :options="kinds" @change="load" />
      <view class="lk-sheet-sub l-muted">{{ t('lookup.price.scope') }}</view>
      <scroll-view scroll-y class="ss-list">
        <view v-if="loading" class="ss-loading l-muted">{{ t('common.loading') }}</view>
        <view v-else-if="!kinds.length || denied" class="ss-loading l-muted">{{ t('lookup.price.noPermission') }}</view>
        <block v-else>
          <view v-for="item in items" :key="item.key" class="ss-row">
            <view class="lk-sheet-line">
              <text class="ss-name">{{ item.title }}</text>
              <text class="lk-sheet-price">{{ item.line ? price(item.line.price) : '-' }}</text>
            </view>
            <text class="ss-sub">{{ item.line ? lineText(item.line) : t('lookup.price.none') }}</text>
          </view>
        </block>
      </scroll-view>
      <view v-if="kinds.length && !denied" class="lk-sheet-actions">
        <text class="lk-sheet-btn" @click="go('trades')">{{ t('lookup.price.moreTrades') }}</text>
        <text class="lk-sheet-btn" @click="go('reference')">{{ t('lookup.price.moreContent') }}</text>
      </view>
      <view class="ss-cancel" @click="visible = false">{{ t('common.cancel') }}</view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from '@/locales'
import { OrderKind } from '@web/constants/enums'
import { lookupSummary } from '@/api/lookup'
import { price, qty } from '@/utils/format'
import { canViewKind } from './lookup'

/**
 * 售价 / 进价历史参考：最近 3 次、系统最新价、最高价、最低价（按看得到的单据统计，优惠前含税单价）。
 * 底部【更多交易历史】联查往来明细（lookup/trades），【更多内容】联查与 Web 端相同的价格参考（lookup/reference，按权限显示标签页）。
 * 只能看售价的人没有「进价」分段。用法：ref.open(row, kind)，kind 为 SALES / PURCHASE
 */
const { t } = useI18n()
const visible = ref(false)
const loading = ref(false)
const denied = ref(false)
const material = ref(null)
const kind = ref(OrderKind.SALES)
const summary = ref(null)

const kinds = computed(() =>
  [
    canViewKind(OrderKind.SALES) ? { value: OrderKind.SALES, label: t('lookup.price.sales') } : null,
    canViewKind(OrderKind.PURCHASE) ? { value: OrderKind.PURCHASE, label: t('lookup.price.purchase') } : null
  ].filter(Boolean)
)

const items = computed(() => {
  const s = summary.value || {}
  const recent = s.recent || []
  return [
    ...[0, 1, 2].map((i) => ({ key: `r${i}`, title: t('lookup.price.recent', { n: i + 1 }), line: recent[i] })),
    { key: 'latest', title: t('lookup.price.latest'), line: s.latest },
    { key: 'max', title: t('lookup.price.max'), line: s.max },
    { key: 'min', title: t('lookup.price.min'), line: s.min }
  ]
})

const lineText = (l) => [l.bizDate, l.partnerName, `${qty(l.qty)} ${material.value?.unit || ''}`.trim()].filter(Boolean).join(' · ')

let seq = 0
async function load() {
  const current = ++seq
  loading.value = true
  denied.value = false
  summary.value = null
  try {
    const res = await lookupSummary(material.value.materialId, kind.value)
    if (current === seq) summary.value = res
  } catch (e) {
    if (current === seq) denied.value = true
  } finally {
    if (current === seq) loading.value = false
  }
}

function go(page) {
  const m = material.value
  visible.value = false
  uni.navigateTo({ url: `/pkg-wms/lookup/${page}?materialId=${m.materialId}&kind=${kind.value}&name=${encodeURIComponent(m.name)}` })
}

defineExpose({
  open(m, k) {
    material.value = m
    const allowed = kinds.value.map((o) => o.value)
    kind.value = allowed.includes(k) ? k : allowed[0] || OrderKind.SALES
    visible.value = true
    if (allowed.length) load()
  }
})
</script>

<style lang="scss">
@import '../sheet.scss';
@import './lookup.scss';
</style>
