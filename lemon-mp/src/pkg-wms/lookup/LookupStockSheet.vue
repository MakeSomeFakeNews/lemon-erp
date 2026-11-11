<template>
  <view v-if="visible" class="ss-mask" @click.self="visible = false">
    <view class="ss-panel">
      <view class="ss-title">{{ t('lookup.stock.title', { name: material?.name || '' }) }}</view>
      <view class="lk-sheet-sub l-muted">{{ t('lookup.stock.total', { n: `${qty(total)} ${material?.unit || ''}` }) }}</view>
      <scroll-view scroll-y class="ss-list">
        <view v-if="loading" class="ss-loading l-muted">{{ t('common.loading') }}</view>
        <block v-else>
          <view v-for="(r, i) in rows" :key="i" class="ss-row">
            <view class="lk-sheet-line">
              <text class="ss-name">{{ r.warehouseName }}{{ r.locationCode ? ` · ${r.locationCode}` : '' }}</text>
              <text class="lk-sheet-qty">{{ qty(r.qty) }}</text>
            </view>
            <text v-if="subOf(r)" class="ss-sub">{{ subOf(r) }}</text>
          </view>
          <l-empty v-if="!rows.length" />
        </block>
      </scroll-view>
      <view class="lk-sheet-actions">
        <text v-if="canHistory" class="lk-sheet-btn" @click="openHistory">{{ t('lookup.stock.history') }}</text>
        <text class="lk-sheet-btn lk-sheet-btn--plain" @click="visible = false">{{ t('common.cancel') }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from '@/locales'
import { lookupStock } from '@/api/lookup'
import { hasPerm } from '@/store/user'
import { qty } from '@/utils/format'

/**
 * 库存分布：各仓库、库位、批次（有效期、冻结）的数量；有库存查询权限时可联查经营历史（进销存流水）。用法：ref.open(row)
 */
const { t } = useI18n()
const visible = ref(false)
const loading = ref(false)
const material = ref(null)
const rows = ref([])

const total = computed(() => rows.value.reduce((s, r) => s + (Number(r.qty) || 0), 0))
const canHistory = computed(() => hasPerm('wms:stock:list'))

function subOf(r) {
  return [
    r.batchNo ? t('lookup.stock.batch', { batch: r.batchNo }) : '',
    r.expiryDate ? t('lookup.stock.expiry', { date: r.expiryDate }) : '',
    Number(r.frozenQty) > 0 ? t('lookup.stock.frozen', { n: qty(r.frozenQty) }) : ''
  ]
    .filter(Boolean)
    .join(' · ')
}

function openHistory() {
  const m = material.value
  visible.value = false
  uni.navigateTo({ url: `/pkg-wms/stock/history?materialId=${m.materialId}&name=${encodeURIComponent(m.name)}` })
}

defineExpose({
  async open(m) {
    material.value = m
    rows.value = []
    visible.value = true
    loading.value = true
    try {
      rows.value = (await lookupStock(m.materialId).catch(() => [])) || []
    } finally {
      loading.value = false
    }
  }
})
</script>

<style lang="scss">
@import '../sheet.scss';
@import './lookup.scss';
</style>
