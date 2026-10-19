<template>
  <view>
    <l-scan-input :placeholder="t('wms.stock.scanTip')" @scan="onScan" />
    <view class="m-current">
      <block v-if="filterText">
        <l-tag :text="filterText" color="blue" />
        <text class="m-link" @click="clearFilter">{{ t('wms.common.clear') }}</text>
      </block>
      <text class="stock__low">{{ t('wms.stock.lowOnly') }}</text>
      <switch :checked="lowStock" style="transform: scale(0.7)" @change="changeLow" />
    </view>
    <view v-for="s in paged.list" :key="s.materialId" class="m-card" @click="toggle(s)">
      <view class="m-row">
        <text class="m-title">{{ s.materialCode }} {{ s.materialName }}</text>
        <l-tag v-if="isLow(s)" :text="t('wms.stock.low')" color="red" />
      </view>
      <view v-if="s.spec" class="m-sub">{{ s.spec }}</view>
      <view class="stock__nums">
        <view class="stock__num">
          <text class="stock__num-value" :class="{ 'stock__num-value--low': isLow(s) }">{{ qty(s.qty) }}</text>
          <text class="stock__num-label">{{ t('wms.stock.qty') }}{{ s.unit ? ` (${s.unit})` : '' }}</text>
        </view>
        <view class="stock__num">
          <text class="stock__num-value">{{ qty(available(s)) }}</text>
          <text class="stock__num-label">{{ t('wms.stock.available') }}</text>
        </view>
        <view class="stock__num">
          <text class="stock__num-value">{{ Number(s.safetyStock) > 0 ? qty(s.safetyStock) : '-' }}</text>
          <text class="stock__num-label">{{ t('wms.stock.safety') }}</text>
        </view>
      </view>
      <!-- 展开：各仓库、库位、批次的库存 -->
      <view v-if="expanded[s.materialId]" class="stock__details">
        <view v-if="expanded[s.materialId].loading" class="m-sub">{{ t('common.loading') }}</view>
        <view v-else-if="!expanded[s.materialId].list.length" class="m-sub">{{ t('common.empty') }}</view>
        <view v-for="d in expanded[s.materialId].list" :key="d.id" class="stock__detail">
          <view class="m-row">
            <text>{{ d.warehouseName }} · {{ locationText(d.locationCode) }}</text>
            <text class="stock__detail-qty">{{ qty(d.qty) }}</text>
          </view>
          <view v-if="d.batchNo || Number(d.frozenQty) > 0 || d.expiryDate" class="m-sub">
            <text v-if="d.batchNo">{{ t('wms.common.batch') }} {{ d.batchNo }}　</text>
            <text v-if="d.expiryDate" :class="{ 'stock__expired': d.expiryDate < todayStr }">{{ t('wms.stock.expiry', { date: d.expiryDate }) }}　</text>
            <text v-if="Number(d.frozenQty) > 0">{{ t('wms.stock.frozen', { n: qty(d.frozenQty) }) }}</text>
          </view>
        </view>
      </view>
    </view>
    <l-empty v-if="paged.loaded && !paged.list.length" />
    <l-load-more v-else-if="paged.list.length" :status="paged.status" />
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { onLoad, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { materialByBarcode, pageStock, pageStockSummary } from '@/api/wms'
import { useTitle } from '@/composables/useTitle'
import { usePaged } from '@/composables/usePaged'
import { vibrate } from '@/composables/useAction'
import { qty, today } from '@/utils/format'
import { availableQty, locationText } from '../mobile'

/**
 * 库存查询：扫描物料条码 / 包装条码，或输入物料编码、名称，按物料汇总显示库存；点开查看各仓库、库位、批次的数量。
 * 低于安全库存的物料标红，可只看低于安全库存的物料
 */
const { t } = useI18n()
useTitle('wms.entry.stock')
const todayStr = today()
const materialId = ref(null)
const keyword = ref('')
const filterText = ref('')
const lowStock = ref(false)
const expanded = reactive({})

const paged = usePaged((p) =>
  pageStockSummary({ ...p, materialId: materialId.value || undefined, keyword: keyword.value || undefined, lowStock: lowStock.value || undefined })
)

const available = (s) => availableQty(s)
const isLow = (s) => Number(s.safetyStock) > 0 && Number(s.qty) < Number(s.safetyStock)

onLoad((q) => {
  lowStock.value = q?.low === '1'
  paged.refresh()
})
onPullDownRefresh(() => reload())
onReachBottom(() => paged.more())

function reload() {
  Object.keys(expanded).forEach((k) => delete expanded[k])
  return paged.refresh()
}

/** 先按条码 / 编码精确匹配物料，匹配不到时按编码、名称模糊查询 */
async function onScan(code) {
  const hit = await materialByBarcode(code).catch(() => null)
  if (hit?.material) {
    materialId.value = hit.material.id
    keyword.value = ''
    filterText.value = `${hit.material.code} ${hit.material.name}`
    vibrate(true)
  } else {
    materialId.value = null
    keyword.value = code
    filterText.value = code
  }
  await reload()
  // 只有一个物料时直接展开明细
  if (paged.list.length === 1) toggle(paged.list[0])
}

function clearFilter() {
  materialId.value = null
  keyword.value = ''
  filterText.value = ''
  reload()
}

function changeLow(e) {
  lowStock.value = e.detail.value
  reload()
}

async function toggle(s) {
  if (expanded[s.materialId]) {
    delete expanded[s.materialId]
    return
  }
  expanded[s.materialId] = { loading: true, list: [] }
  try {
    const res = await pageStock({ materialId: s.materialId, current: 1, size: 200 })
    expanded[s.materialId] = { loading: false, list: res.records || [] }
  } catch {
    delete expanded[s.materialId]
  }
}
</script>

<style lang="scss">
@import '../mobile.scss';

.stock {
  &__low {
    margin-left: auto;
  }
  &__nums {
    display: flex;
    margin-top: 20rpx;
  }
  &__num {
    flex: 1;
    display: flex;
    flex-direction: column;
  }
  &__num-value {
    font-size: 34rpx;
    font-weight: 600;
    &--low {
      color: $l-danger;
    }
  }
  &__num-label {
    margin-top: 4rpx;
    font-size: 22rpx;
    color: $l-text-3;
  }
  &__details {
    margin-top: 20rpx;
    padding-top: 8rpx;
    border-top: 1rpx solid $l-border;
  }
  &__detail {
    padding: 16rpx 0;
    font-size: 26rpx;
    border-bottom: 1rpx solid $l-border;
    &:last-child {
      border-bottom: none;
    }
  }
  &__detail-qty {
    font-weight: 600;
  }
  &__expired {
    color: $l-danger;
  }
}
</style>
