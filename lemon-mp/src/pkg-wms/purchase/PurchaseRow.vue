<template>
  <view class="m-card" :class="{ 'm-card--active': active }" @click="$emit('activate')">
    <view class="m-row">
      <text class="m-title">{{ row.materialCode }} {{ row.materialName }}</text>
      <text class="m-link pur-row__remove" @click.stop="$emit('remove')">{{ t('common.delete') }}</text>
    </view>
    <view v-if="row.spec" class="m-sub">{{ row.spec }}</view>

    <!-- 数量（可选辅助单位）、含税单价 -->
    <view class="m-row">
      <view class="m-input pur-row__half" :class="{ 'm-input--disabled': row.serialEnabled === 1 }">
        <text class="m-input__label">{{ t('wms.common.qty') }}</text>
        <input :value="row.qty" type="digit" :disabled="row.serialEnabled === 1" @blur="(e) => changeQty(e.detail.value)" />
        <picker v-if="unitChoices.length > 1" :range="unitChoices" range-key="label" :value="unitIndex" @change="pickUnit">
          <text class="m-link">{{ unitChoices[unitIndex].unit || row.unit }} ▾</text>
        </picker>
        <text v-else class="l-muted">{{ row.unit }}</text>
      </view>
      <view class="m-input pur-row__half">
        <text class="m-input__label">{{ t('wms.sale.price') }}</text>
        <input :value="row.price" type="digit" :placeholder="t('common.pleaseInput')" placeholder-class="l-placeholder" @blur="(e) => changePrice(e.detail.value)" />
      </view>
    </view>
    <view v-if="row.unitName" class="m-sub">
      {{ t('wms.sale.baseEquals', { qty: qty(baseQty(row)), unit: row.unit || '', price: row.price == null ? '-' : price(basePrice(row)) }) }}
    </view>
    <view v-if="row.priceSource && row.priceAuto" class="m-sub">{{ t('wms.sale.priceSource', { source: t('wms.sale.source.' + row.priceSource) }) }}</view>

    <!-- 库位（空则按上架策略推荐）、批次、有效期 -->
    <picker :range="locations" range-key="label" :value="locationIndex(locations, row.locationId)" @change="(e) => (row.locationId = locations[e.detail.value].id)">
      <view class="m-input">
        <text class="m-input__label">{{ t('wms.common.location') }}</text>
        <text class="m-input__value" :class="{ 'l-placeholder': !row.locationId }">{{ locations[locationIndex(locations, row.locationId)].label }}</text>
      </view>
    </picker>
    <view v-if="row.batchEnabled === 1" class="m-row">
      <view class="m-input pur-row__half">
        <text class="m-input__label">{{ t('wms.common.batch') }}</text>
        <input v-model="row.batchNo" maxlength="64" :placeholder="t('trade.purchase.batchRequired')" placeholder-class="l-placeholder" />
      </view>
      <picker class="pur-row__half" mode="date" :value="row.expiryDate || ''" @change="(e) => (row.expiryDate = e.detail.value)">
        <view class="m-input">
          <text class="m-input__label">{{ t('trade.purchase.expiry') }}</text>
          <text class="m-input__value" :class="{ 'l-placeholder': !row.expiryDate }">{{ row.expiryDate || t('wms.sale.optional') }}</text>
        </view>
      </picker>
    </view>
    <view v-if="row.serialEnabled === 1" class="m-serials">
      <text>{{ t('wms.common.selectThenScanSerial') }}</text>
      <text v-for="s in row.serials" :key="s" class="m-serial" @click.stop="$emit('remove-serial', s)">{{ s }}</text>
    </view>
    <view class="m-row pur-row__amount">
      <view class="pur-row__tax" @click.stop>
        <text class="l-muted">{{ t('wms.sale.taxRateLabel') }}</text>
        <input class="pur-row__tax-input" :value="row.taxRate ?? ''" type="digit" :placeholder="t('wms.sale.taxRateDefault')"
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
import { locationIndex, toNumber } from '../mobile'
import { baseQty, basePrice, changeUnit, lineAmount, unitRange } from '../sale/lines'

/**
 * 快速入库的一行明细：数量（可选辅助单位）、含税单价、税率、库位（空则推荐）、批次与有效期、序列号。直接修改 row
 * 事件：activate 选中（扫序列号用）、remove、remove-serial、changed 数量 / 单位变化（重新带出默认价）
 */
const props = defineProps({
  row: { type: Object, required: true },
  active: Boolean,
  /** 库位选项（locationRange 的结果，第一项为空） */
  locations: { type: Array, default: () => [] }
})
const emit = defineEmits(['activate', 'remove', 'remove-serial', 'changed'])
const { t } = useI18n()

/** 序列号物料只按基本单位 */
const unitChoices = computed(() => (props.row.serialEnabled === 1 ? [] : unitRange(props.row)))
const unitIndex = computed(() => Math.max(0, unitChoices.value.findIndex((u) => u.unit === (props.row.unitName || ''))))

function pickUnit(e) {
  changeUnit(props.row, unitChoices.value[Number(e.detail.value)])
  emit('changed')
}

function changeQty(v) {
  props.row.qty = toNumber(v, 0) || 0
  emit('changed')
}

function changePrice(v) {
  const n = v === '' ? null : Number(v)
  props.row.price = Number.isFinite(n) && n >= 0 ? Math.round(n * 10000) / 10000 : null
  props.row.priceAuto = false
}

function changeTaxRate(v) {
  const n = v === '' ? null : Number(v)
  props.row.taxRate = Number.isFinite(n) && n >= 0 && n <= 100 ? Math.round(n * 100) / 100 : null
}
</script>

<style lang="scss">
@import '../mobile.scss';

.pur-row {
  &__remove {
    color: $l-danger;
  }
  &__half {
    flex: 1;
    min-width: 0;
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
.pur-row__tax {
  display: flex;
  align-items: center;
  gap: 8rpx;
}
.pur-row__tax-input {
  width: 120rpx;
  text-align: right;
}
</style>
