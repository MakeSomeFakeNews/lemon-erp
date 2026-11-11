<template>
  <view class="m-card" :class="{ 'm-card--active': active }" @click="$emit('activate')">
    <view class="m-row">
      <text class="m-title">
        <text v-if="row.gift" class="sale-row__gift">{{ t('wms.sale.gift') }}</text>
        {{ row.materialCode }} {{ row.materialName }}
      </text>
      <text v-if="row.gift && lineWarehouse" class="m-link" @click.stop="$emit('split')">{{ t('wms.sale.giftSplit') }}</text>
      <text v-if="!row.gift || (giftSplit && !giftFirst)" class="m-link sale-row__remove" @click.stop="$emit('remove')">{{ t('common.delete') }}</text>
    </view>
    <view v-if="row.spec" class="m-sub">{{ row.spec }}</view>
    <view v-if="row.gift" class="m-sub sale-row__promo">
      {{ t('wms.sale.giftOf', { name: row.promotionName || '' }) }}{{ giftSplit ? t('wms.sale.giftTotal', { n: qty(row.giftTotal) }) : '' }}
    </view>

    <!-- 数量（可选辅助单位）、单价 -->
    <view class="m-row">
      <view class="m-input sale-row__half" :class="{ 'm-input--disabled': qtyLocked }">
        <text class="m-input__label">{{ t('wms.common.qty') }}</text>
        <input :value="row.qty" type="digit" :disabled="qtyLocked" @blur="(e) => changeQty(e.detail.value)" />
        <picker v-if="unitChoices.length > 1" :range="unitChoices" range-key="label" :value="unitIndex" @change="pickUnit">
          <text class="m-link">{{ unitChoices[unitIndex].unit || row.unit }} ▾</text>
        </picker>
        <text v-else class="l-muted">{{ row.unit }}</text>
      </view>
      <view v-if="!row.gift" class="m-input sale-row__half">
        <text class="m-input__label">{{ t('wms.sale.price') }}</text>
        <input :value="row.price" type="digit" :placeholder="t('common.pleaseInput')" placeholder-class="l-placeholder" @blur="(e) => changePrice(e.detail.value)" />
      </view>
    </view>
    <view v-if="row.unitName" class="m-sub">
      {{ t('wms.sale.baseEquals', { qty: qty(baseQty(row)), unit: row.unit || '', price: row.price == null ? '-' : price(basePrice(row)) }) }}
    </view>
    <view v-if="!row.gift" class="m-row m-sub">
      <text>{{ row.priceSource && row.priceAuto ? t('wms.sale.priceSource', { source: t('wms.sale.source.' + row.priceSource) }) : '' }}</text>
      <text class="m-link" @click.stop="$emit('reference')">{{ t('wms.sale.priceRef') }}</text>
    </view>
    <view v-if="promoLine" class="sale-row__promo">
      {{ t('wms.sale.promoPrice', { price: price(promoLine.price) }) }}{{ promoLine.promotionName ? ` (${promoLine.promotionName})` : '' }}
    </view>

    <!-- 明细仓库（业务参数 scm.salesOut.lineWarehouse） -->
    <picker v-if="lineWarehouse" :range="warehouseChoices" range-key="name" :value="warehouseIndex" @change="pickWarehouse">
      <view class="m-input">
        <text class="m-input__label">{{ t('wms.common.warehouse') }}</text>
        <text class="m-input__value" :class="{ 'l-placeholder': !row.warehouseId }">{{ warehouseChoices[warehouseIndex].name }}</text>
      </view>
    </picker>

    <picker v-if="row.slots.length" :range="row.slots" range-key="label" :value="row.slotIndex" @change="(e) => (row.slotIndex = Number(e.detail.value))">
      <view class="m-input">
        <text class="m-input__label">{{ t('wms.sale.slot') }}</text>
        <text class="m-input__value" :class="{ 'qty-short': overStock(row) }">{{ row.slots[row.slotIndex]?.label }}</text>
      </view>
    </picker>
    <view v-if="row.gift && Number(row.qty) > 0 && !row.slotsLoading && warehouseShort" class="m-sub qty-short">
      {{ lineWarehouse ? t('wms.sale.giftSplitTip') : t('wms.sale.giftShortTip') }}
    </view>
    <view v-else-if="row.gift && row.serialEnabled !== 1 && row.slots.length && overStock(row)" class="m-sub">{{ t('wms.sale.giftAutoSplit') }}</view>
    <view v-if="!row.slots.length" class="m-sub sale-row__nostock">
      {{ row.slotsLoading ? t('common.loading') : warehouseId ? t('wms.sale.noStock') : t('wms.common.selectWarehouseFirst') }}
    </view>
    <view v-if="row.serialEnabled === 1" class="m-serials">
      <text>{{ t('wms.common.selectThenScanSerial') }}</text>
      <text v-for="s in row.serials" :key="s" class="m-serial" @click.stop="$emit('remove-serial', s)">{{ s }}</text>
    </view>
    <view v-if="!row.gift" class="m-row sale-row__amount">
      <!-- 税率可逐行修改；为空时后端按发票类型 / 默认税率 -->
      <view class="sale-row__tax" @click.stop>
        <text class="l-muted">{{ t('wms.sale.taxRateLabel') }}</text>
        <input class="sale-row__tax-input" :value="row.taxRate ?? ''" type="digit" :placeholder="t('wms.sale.taxRateDefault')"
          placeholder-class="l-placeholder" @blur="(e) => changeTaxRate(e.detail.value)" />
        <text class="l-muted">%</text>
      </view>
      <text class="m-strong">{{ money(lineAmount(row)) }}</text>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from '@/locales'
import { money, price, qty } from '@/utils/format'
import { toNumber } from '../mobile'
import { baseQty, basePrice, changeUnit, lineAmount, overStock, unitRange } from './lines'

/**
 * 快速开单的一行明细：数量（可选辅助单位）、单价、明细仓库、库位批次、序列号。直接修改 row；
 * 赠品行（row.gift）的数量由促销决定，只能选仓库、库位批次和序列号
 *
 * 事件：activate 选中（扫序列号用）、remove、remove-serial、reference 价格参考、
 * changed 数量 / 单位变化（重新带出默认价）、warehouse 明细仓库变化（重新加载库位批次）
 */
const props = defineProps({
  row: { type: Object, required: true },
  active: Boolean,
  /** 明细可选仓库 */
  lineWarehouse: Boolean,
  /** 仓库选项 [{ id, name }] */
  warehouses: { type: Array, default: () => [] },
  /** 本行实际出库的仓库（明细仓库，为空取表头仓库） */
  warehouseId: { type: [Number, String], default: null },
  /** 促销预览中本行的促销价 */
  promoLine: { type: Object, default: null },
  /** 赠品已拆成多行（数量可在行之间调整） */
  giftSplit: Boolean,
  /** 赠品组的第一行（不能删除） */
  giftFirst: Boolean
})
const emit = defineEmits(['activate', 'remove', 'remove-serial', 'reference', 'changed', 'warehouse', 'split', 'gift-qty'])
const { t } = useI18n()

/** 序列号物料的数量 = 序列号个数；赠品数量由促销决定，拆成多行后可在行之间调整 */
const qtyLocked = computed(() => (props.row.gift ? !props.giftSplit : props.row.serialEnabled === 1))

/** 本行仓库的可用库存合计不够本行赠品数量 */
const warehouseShort = computed(() => props.row.slots.reduce((sum, s) => sum + s.available, 0) < Number(props.row.qty))

/** 序列号物料、赠品只按基本单位 */
const unitChoices = computed(() => (props.row.gift || props.row.serialEnabled === 1 ? [] : unitRange(props.row)))
const unitIndex = computed(() => Math.max(0, unitChoices.value.findIndex((u) => u.unit === (props.row.unitName || ''))))

function pickUnit(e) {
  changeUnit(props.row, unitChoices.value[Number(e.detail.value)])
  emit('changed')
}

function changeQty(v) {
  if (props.row.gift) return emit('gift-qty', v)
  props.row.qty = toNumber(v, 0) || 0
  emit('changed')
}

function changePrice(v) {
  const n = v === '' ? null : Number(v)
  props.row.price = Number.isFinite(n) && n >= 0 ? Math.round(n * 10000) / 10000 : null
  props.row.priceAuto = false
}

/** 税率 0 ~ 100，最多 2 位小数；清空为按默认 */
function changeTaxRate(v) {
  const n = v === '' ? null : Number(v)
  props.row.taxRate = Number.isFinite(n) && n >= 0 && n <= 100 ? Math.round(n * 100) / 100 : null
}

const warehouseChoices = computed(() => [{ id: null, name: t('wms.sale.sameAsHeader') }, ...props.warehouses])
const warehouseIndex = computed(() => Math.max(0, warehouseChoices.value.findIndex((w) => w.id === (props.row.warehouseId || null))))

function pickWarehouse(e) {
  const id = warehouseChoices.value[Number(e.detail.value)]?.id || null
  if (id === (props.row.warehouseId || null)) return
  props.row.warehouseId = id
  props.row.serials = []
  if (props.row.serialEnabled === 1) props.row.qty = 0
  emit('warehouse')
}
</script>

<style lang="scss">
/* 小程序组件样式隔离：引入扫码作业的公共样式 */
@import '../mobile.scss';

.sale-row {
  &__remove {
    color: $l-danger;
  }
  &__gift {
    margin-right: 8rpx;
    padding: 2rpx 12rpx;
    border-radius: 6rpx;
    background: $l-success;
    color: #fff;
    font-size: 22rpx;
  }
  &__half {
    flex: 1;
    min-width: 0;
  }
  &__promo {
    margin-top: 8rpx;
    font-size: 24rpx;
    color: $l-success;
  }
  &__nostock {
    color: $l-danger;
  }
  &__amount {
    margin-top: 16rpx;
    font-size: 26rpx;
    .m-strong {
      font-size: 30rpx;
      font-weight: 600;
    }
  }
}
.sale-row__tax {
  display: flex;
  align-items: center;
  gap: 8rpx;
}
.sale-row__tax-input {
  width: 120rpx;
  text-align: right;
}
</style>
