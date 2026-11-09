<template>
  <view v-if="visible" class="ss-mask" @click.self="close">
    <view class="ss-panel">
      <view class="ss-title">{{ t('wms.sale.refTitle', { name: material?.materialName || '' }) }}</view>
      <view class="pr-tip l-muted">{{ t('wms.sale.refTip') }}</view>
      <scroll-view scroll-y class="ss-list">
        <view v-if="loading" class="ss-loading l-muted">{{ t('common.loading') }}</view>
        <view v-else-if="!allowed" class="ss-loading l-muted">{{ t('wms.sale.refNoPermission') }}</view>
        <block v-else>
          <view v-for="sec in sections" :key="sec.key">
            <view class="pr-section">{{ sec.title }}</view>
            <view v-for="(r, i) in sec.rows" :key="i" class="ss-row" @click="pick(r.price)">
              <view class="pr-row">
                <text class="ss-name">{{ r.bizDate }}{{ sec.showPartner ? ` · ${r.partnerName}` : '' }}</text>
                <text class="pr-price">{{ price(r.price) }}</text>
              </view>
              <text class="ss-sub">{{ [r.docNo, t('wms.sale.refQty', { n: qty(r.qty) })].filter(Boolean).join(' · ') }}</text>
            </view>
            <view v-if="!sec.rows.length" class="pr-empty l-muted">{{ t('common.empty') }}</view>
          </view>
        </block>
      </scroll-view>
      <view class="ss-cancel" @click="close">{{ t('common.cancel') }}</view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from '@/locales'
import { priceReference } from '@/api/wms'
import { price, qty } from '@/utils/format'

/**
 * 简化的价格参考（与 Web 端 PriceReference 相同的接口，只取销售历史）：该客户的历史售价、最近售价（全部客户），点击价格选用。
 * 价格为含税、基本单位、本位币。用法：ref.open({ materialId, materialName, partnerId })，监听 pick(price)
 */
const emit = defineEmits(['pick'])
const { t } = useI18n()
const ROWS = 10

const visible = ref(false)
const loading = ref(false)
const material = ref(null)
const mine = ref(null)
const all = ref(null)

/** 后端按权限裁剪分区：没有 sales 分区即没有权限 */
const allowed = computed(() => Array.isArray(all.value?.sales))
const sections = computed(() =>
  [
    material.value?.partnerId ? { key: 'mine', title: t('wms.sale.refCustomer'), rows: (mine.value?.sales || []).slice(0, ROWS), showPartner: false } : null,
    { key: 'all', title: t('wms.sale.refRecent'), rows: (all.value?.sales || []).slice(0, ROWS), showPartner: true }
  ].filter(Boolean)
)

let seq = 0
async function load() {
  const current = ++seq
  const m = material.value
  loading.value = true
  try {
    const [a, b] = await Promise.all([
      priceReference({ materialId: m.materialId, kind: 'SALES' }).catch(() => null),
      m.partnerId ? priceReference({ materialId: m.materialId, partnerId: m.partnerId, kind: 'SALES' }).catch(() => null) : null
    ])
    if (current !== seq) return
    all.value = a
    mine.value = b
  } finally {
    if (current === seq) loading.value = false
  }
}

function pick(value) {
  if (value == null) return
  emit('pick', Number(value))
  close()
}

function close() {
  visible.value = false
}

defineExpose({
  open(m) {
    material.value = m
    all.value = null
    mine.value = null
    visible.value = true
    load()
  }
})
</script>

<style lang="scss">
@import '../sheet.scss';

.pr-tip {
  padding: 8rpx 32rpx 16rpx;
  font-size: 24rpx;
}
.pr-section {
  padding: 16rpx 32rpx 12rpx;
  font-size: 26rpx;
  font-weight: 600;
  color: $l-text-2;
}
.pr-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}
.pr-price {
  font-size: 30rpx;
  font-weight: 600;
  color: $l-primary;
}
.pr-empty {
  padding: 0 32rpx 16rpx;
  font-size: 24rpx;
}
</style>
