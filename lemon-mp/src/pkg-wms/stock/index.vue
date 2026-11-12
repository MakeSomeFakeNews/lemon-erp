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
      <text>{{ t('wms.stock.highOnly') }}</text>
      <switch :checked="highStock" style="transform: scale(0.7)" @change="changeHigh" />
    </view>
    <view v-for="s in paged.list" :key="s.materialId" class="m-card" @click="toggle(s)">
      <view class="stock__head">
        <!-- 商品主图，点击查看大图 -->
        <image v-if="s.imageUrl" class="stock__img" :src="fileUrl(s.imageUrl)" mode="aspectFill" @click.stop="preview(s)" />
        <view class="stock__head-text">
          <view class="m-row">
            <text class="m-title">{{ s.materialCode }} {{ s.materialName }}</text>
            <l-tag v-if="alertText(s)" :text="alertText(s)" :color="isLow(s) ? 'red' : 'orange'" />
          </view>
          <view v-if="s.spec" class="m-sub">{{ s.spec }}</view>
        </view>
      </view>
      <view class="stock__nums">
        <view class="stock__num">
          <text class="stock__num-value" :class="{ 'stock__num-value--low': isLow(s), 'stock__num-value--high': isHigh(s) }">{{ qty(s.qty) }}</text>
          <text class="stock__num-label">{{ t('wms.stock.qty') }}{{ s.unit ? ` (${s.unit})` : '' }}</text>
        </view>
        <view class="stock__num">
          <text class="stock__num-value">{{ qty(available(s)) }}</text>
          <text class="stock__num-label">{{ t('wms.stock.available') }}</text>
        </view>
        <view class="stock__num">
          <text class="stock__num-value">{{ limitText(s) }}</text>
          <text class="stock__num-label">{{ t('wms.stock.limits') }}{{ s.alertMode === StockAlertMode.WAREHOUSE ? t('wms.stock.perWarehouse') : '' }}</text>
        </view>
      </view>
      <!-- 均价、库存金额：有「查看成本金额」权限时后端才返回 -->
      <view v-if="s.unitCost != null || s.amount != null" class="m-row stock__cost">
        <text>{{ t('wms.stock.unitCost') }} {{ price(s.unitCost) }}</text>
        <text>{{ t('wms.stock.amount') }} <text class="stock__amount">{{ money(s.amount) }}</text></text>
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
            <text v-if="d.productionDate">{{ t('wms.stock.produced', { date: d.productionDate }) }}　</text>
            <text v-if="d.expiryDate" :class="{ 'stock__expired': d.expiryDate < todayStr }">{{ t('wms.stock.expiry', { date: d.expiryDate }) }}　</text>
            <text v-if="Number(d.frozenQty) > 0">{{ t('wms.stock.frozen', { n: qty(d.frozenQty) }) }}</text>
          </view>
        </view>
        <view v-if="!expanded[s.materialId].loading" class="stock__history" @click.stop="openHistory(s)">{{ t('wms.history.open') }} ›</view>
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
import { money, price, qty, today } from '@/utils/format'
import { availableQty, locationText } from '../mobile'
import { fileUrl } from '@/utils/request'
import { StockAlert, StockAlertMode } from '@web/constants/enums'

/**
 * 库存查询：扫描物料条码 / 包装条码，或输入物料编码、名称，按物料汇总显示库存；点开查看各仓库、库位、批次的数量。
 * 低于下限的物料标红、高于上限的标橙（分仓库预警显示低于 / 高于的仓库数），可只看低于下限或高于上限的物料；有「查看成本金额」权限时显示均价、库存金额。展开后可进入经营历史
 */
const { t } = useI18n()
useTitle('wms.entry.stock')
const todayStr = today()
const materialId = ref(null)
const keyword = ref('')
const filterText = ref('')
const lowStock = ref(false)
const highStock = ref(false)
const expanded = reactive({})

const paged = usePaged((p) =>
  pageStockSummary({ ...p, materialId: materialId.value || undefined, keyword: keyword.value || undefined, lowStock: lowStock.value || undefined,
    highStock: highStock.value || undefined })
)

const available = (s) => availableQty(s)
// 预警由后端按物料的预警方式判断：按总库存比较所有仓库的合计；分仓库按仓库判断，给出低于 / 高于的仓库数
const isLow = (s) => s.stockAlert === StockAlert.LOW
const isHigh = (s) => s.stockAlert === StockAlert.HIGH
function alertText(s) {
  if (s.alertMode === StockAlertMode.WAREHOUSE) {
    if (s.lowWarehouses) return t('wms.stock.lowWarehouses', { n: s.lowWarehouses })
    if (s.highWarehouses) return t('wms.stock.highWarehouses', { n: s.highWarehouses })
    return ''
  }
  return isLow(s) ? t('wms.stock.low') : isHigh(s) ? t('wms.stock.high') : ''
}
/** 下限 ~ 上限（分仓库时为各仓库之和），都没有设置时为 - */
const limitText = (s) => (s.safetyStock == null && s.maxStock == null ? '-' : `${s.safetyStock == null ? '-' : qty(s.safetyStock)} ~ ${s.maxStock == null ? '-' : qty(s.maxStock)}`)

onLoad((q) => {
  // 首页待办「库存低于安全库存」「库存高于库存上限」带 lowStock=1 / highStock=1（与 Web 端参数相同）
  lowStock.value = q?.lowStock === '1'
  highStock.value = q?.highStock === '1'
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

function changeHigh(e) {
  highStock.value = e.detail.value
  reload()
}

/** 经营历史：期初、流水、结余（与 Web 端相同的接口） */
/** 查看商品主图大图 */
function preview(s) {
  const url = fileUrl(s.imageUrl)
  uni.previewImage({ urls: [url], current: url })
}

function openHistory(s) {
  const name = encodeURIComponent(`${s.materialCode} ${s.materialName}`)
  uni.navigateTo({ url: `/pkg-wms/stock/history?materialId=${s.materialId}&name=${name}` })
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
  &__head {
    display: flex;
    align-items: center;
  }
  &__head-text {
    flex: 1;
    min-width: 0;
  }
  &__img {
    flex-shrink: 0;
    width: 96rpx;
    height: 96rpx;
    margin-right: 20rpx;
    border-radius: 8rpx;
    background: #f5f5f5;
  }
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
    &--high {
      color: #fa8c16;
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
  &__cost {
    margin-top: 12rpx;
    font-size: 24rpx;
    color: $l-text-2;
  }
  &__amount {
    font-weight: 600;
    color: $l-text;
  }
  &__history {
    padding-top: 16rpx;
    font-size: 26rpx;
    color: $l-primary;
    text-align: right;
  }
}
</style>
