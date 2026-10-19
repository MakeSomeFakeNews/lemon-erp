<template>
  <view>
    <!-- 选择采购订单 -->
    <block v-if="!order">
      <l-scan-input :placeholder="t('wms.receive.scanOrder')" @scan="searchOrders" />
      <view v-for="o in orders" :key="o.id" class="m-card" @click="selectOrder(o.id)">
        <view class="m-row">
          <text class="m-title">{{ o.orderNo }}</text>
          <l-tag :text="dictText(dict.scm_order_status, o.status)" :color="dictColor(dict.scm_order_status, o.status)" />
        </view>
        <view class="m-row m-sub">
          <text>{{ o.partnerName }}</text>
          <text>{{ o.orderDate }}</text>
        </view>
      </view>
      <l-empty v-if="loaded && !orders.length" :text="t('wms.receive.noOrders')" />
    </block>

    <!-- 扫码收货 -->
    <block v-else>
      <view class="m-card">
        <view class="m-row">
          <text class="m-title">{{ order.orderNo }}</text>
          <text class="m-link" @click="reset">{{ t('wms.receive.changeOrder') }}</text>
        </view>
        <view class="m-sub">{{ order.partnerName }}</view>
        <picker :range="warehouses" range-key="name" :value="warehouseIndex" @change="changeWarehouse">
          <view class="m-input">
            <text class="m-input__label">{{ t('wms.receive.warehouse') }}</text>
            <text class="m-input__value" :class="{ 'l-placeholder': !warehouseId }">{{ warehouseName || t('common.pleaseSelect') }}</text>
          </view>
        </picker>
      </view>
      <l-scan-input :placeholder="t('wms.receive.scanTip')" @scan="onScan" />
      <view v-if="tip.text" class="m-tip" :class="{ 'm-tip--fail': !tip.ok }">{{ tip.text }}</view>
      <view v-if="currentLocation" class="m-current">
        <text>{{ t('wms.common.currentLocation') }}</text>
        <l-tag :text="currentLocation.code" color="blue" />
        <text class="m-link" @click="currentLocation = null">{{ t('wms.common.clear') }}</text>
      </view>
      <view v-for="row in rows" :key="row.orderItemId" class="m-card" :class="{ 'm-card--active': row === activeRow }" @click="activeRow = row">
        <view class="m-row">
          <text class="m-title">{{ row.materialCode }} {{ row.materialName }}</text>
          <text :class="row.qty >= row.limit ? 'qty-done' : ''">{{ qty(row.qty) }} / {{ qty(row.limit) }} {{ row.unit }}</text>
        </view>
        <view v-if="row.spec" class="m-sub">{{ row.spec }}</view>
        <view class="m-input" :class="{ 'm-input--disabled': row.serialEnabled === 1 }">
          <text class="m-input__label">{{ t('wms.common.qty') }}</text>
          <input
            :value="row.qty"
            type="digit"
            :disabled="row.serialEnabled === 1"
            @blur="(e) => (row.qty = toNumber(e.detail.value, 0, row.limit) || 0)"
          />
        </view>
        <picker :range="locRange" range-key="label" :value="locationIndex(locRange, row.locationId)" @change="(e) => (row.locationId = locRange[e.detail.value].id)">
          <view class="m-input">
            <text class="m-input__label">{{ t('wms.common.location') }}</text>
            <text class="m-input__value" :class="{ 'l-placeholder': !row.locationId }">{{ locRange[locationIndex(locRange, row.locationId)].label }}</text>
          </view>
        </picker>
        <view v-if="row.batchEnabled === 1" class="m-input">
          <text class="m-input__label">{{ t('wms.common.batch') }}</text>
          <input v-model="row.batchNo" maxlength="64" :placeholder="t('common.pleaseInput')" placeholder-class="l-placeholder" />
        </view>
        <view v-if="row.serialEnabled === 1" class="m-serials">
          <text>{{ t('wms.common.selectThenScanSerial') }}</text>
          <text v-for="s in row.serials" :key="s" class="m-serial" @click.stop="removeSerial(row, s)">{{ s }}</text>
        </view>
      </view>
      <view class="l-footer-space" />
      <view class="l-footer">
        <view class="m-footer-info">
          {{ t('wms.receive.thisTime') }}<text class="m-strong">{{ qty(totalQty) }}</text>
        </view>
        <button class="l-btn" :loading="saving" :disabled="saving || !totalQty" @click="submit">{{ t('wms.receive.submit') }}</button>
      </view>
    </block>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { OrderStatus } from '@web/constants/enums'
import { addPurchaseIn, getPurchaseOrder, locationOptions, pagePurchaseOrder, warehouseOptions } from '@/api/wms'
import { useTitle } from '@/composables/useTitle'
import { runAction } from '@/composables/useAction'
import { dictColor, dictText, useDict } from '@/store/dict'
import { qty, today } from '@/utils/format'
import { fillPutaway, locationIndex, locationRange, openQty, qty4, resolveScan, toNumber, useScanTip } from '../mobile'

/**
 * 扫码收货：选择采购订单后逐个扫描物料条码累计数量（包装条码按换算比例），
 * 序列号物料选中后扫描序列号；没有指定库位的按上架策略推荐，生成采购入库单
 */
const { t } = useI18n()
useTitle('wms.entry.receive')
const dict = useDict('scm_order_status')
const tip = useScanTip()
const loaded = ref(false)
const orders = ref([])
const order = ref(null)
const warehouses = ref([])
const warehouseId = ref(null)
const locations = ref([])
const currentLocation = ref(null)
const rows = ref([])
const activeRow = ref(null)
const saving = ref(false)

const warehouseIndex = computed(() => Math.max(0, warehouses.value.findIndex((w) => w.id === warehouseId.value)))
const warehouseName = computed(() => warehouses.value.find((w) => w.id === warehouseId.value)?.name)
const locRange = computed(() => locationRange(locations.value, t('wms.receive.locationPh')))
const totalQty = computed(() => qty4(rows.value.reduce((s, r) => s + (Number(r.qty) || 0), 0)))

onLoad(() => {
  warehouseOptions().then((list) => (warehouses.value = list || [])).catch(() => {})
  searchOrders()
})

async function searchOrders(orderNo) {
  try {
    const res = await pagePurchaseOrder({ orderNo: orderNo || undefined, current: 1, size: 50 })
    orders.value = (res.records || []).filter((o) => [OrderStatus.APPROVED, OrderStatus.PARTIAL].includes(o.status))
    if (orderNo && orders.value.length === 1) selectOrder(orders.value[0].id)
  } finally {
    loaded.value = true
  }
}

async function selectOrder(id) {
  const o = await getPurchaseOrder(id)
  const list = (o.items || [])
    .filter((i) => openQty(i) > 0)
    .map((i) => ({
      orderItemId: i.id, materialId: i.materialId, materialCode: i.materialCode, materialName: i.materialName, spec: i.spec, unit: i.unit,
      batchEnabled: i.batchEnabled, serialEnabled: i.serialEnabled, limit: openQty(i), qty: 0, locationId: null, batchNo: '', serials: []
    }))
  if (!list.length) return uni.showToast({ title: t('wms.receive.noOpenItems'), icon: 'none' })
  rows.value = list
  order.value = o
  warehouseId.value = o.warehouseId || null
  activeRow.value = null
  tip.text = ''
  loadLocations()
}

async function loadLocations() {
  locations.value = warehouseId.value ? await locationOptions(warehouseId.value) : []
  currentLocation.value = null
  rows.value.forEach((r) => (r.locationId = null))
}

function changeWarehouse(e) {
  warehouseId.value = warehouses.value[e.detail.value]?.id || null
  loadLocations()
}

function reset() {
  order.value = null
  searchOrders()
}

async function onScan(code) {
  const hit = await resolveScan(code, locations.value)
  if (hit.type === 'location') {
    currentLocation.value = hit.location
    if (activeRow.value && !activeRow.value.locationId) activeRow.value.locationId = hit.location.id
    return tip.show(t('wms.common.currentLocationN', { code: hit.location.code }))
  }
  if (hit.type === 'material') {
    const candidates = rows.value.filter((r) => r.materialId === hit.material.id)
    if (!candidates.length) return tip.show(t('wms.receive.notInOrder', { code: hit.material.code }), false)
    const row = candidates.find((r) => r.qty < r.limit) || candidates[0]
    activeRow.value = row
    if (currentLocation.value && !row.locationId) row.locationId = currentLocation.value.id
    if (row.serialEnabled === 1) return tip.show(t('wms.common.scanSerial', { name: row.materialName }))
    if (qty4(row.qty + hit.ratio) > row.limit) return tip.show(t('wms.receive.overQty', { name: row.materialName }), false)
    row.qty = qty4(row.qty + hit.ratio)
    const added = `${qty(hit.ratio)} ${row.unit || ''}${hit.ratio !== 1 ? ` (1 ${hit.unit || ''})` : ''}`
    return tip.show(t('wms.receive.added', { name: row.materialName, qty: added, total: qty(row.qty) }))
  }
  // 其他条码：作为当前序列号物料的序列号
  const row = activeRow.value
  if (row?.serialEnabled !== 1) return tip.show(t('wms.common.unknownCode', { code }), false)
  if (row.serials.includes(code)) return tip.show(t('wms.common.serialScanned', { code }), false)
  if (row.serials.length >= row.limit) return tip.show(t('wms.receive.overQty', { name: row.materialName }), false)
  row.serials.push(code)
  row.qty = row.serials.length
  tip.show(t('wms.common.serialAdded', { name: row.materialName, code, n: row.qty }))
}

function removeSerial(row, s) {
  row.serials = row.serials.filter((x) => x !== s)
  row.qty = row.serials.length
}

async function submit() {
  if (!warehouseId.value) return uni.showToast({ title: t('wms.receive.selectWarehouse'), icon: 'none' })
  const list = rows.value.filter((r) => Number(r.qty) > 0)
  const noBatch = list.find((r) => r.batchEnabled === 1 && !r.batchNo)
  if (noBatch) return uni.showToast({ title: t('wms.common.inputBatch', { name: noBatch.materialName }), icon: 'none' })
  saving.value = true
  try {
    await runAction(
      async () => {
        await fillPutaway(warehouseId.value, list)
        await addPurchaseIn({
          orderId: order.value.id,
          warehouseId: warehouseId.value,
          bizDate: today(),
          items: list.map((r) => ({
            orderItemId: r.orderItemId, locationId: r.locationId || null, batchNo: r.batchNo || null, qty: r.qty,
            serialNos: r.serialEnabled === 1 ? r.serials.join(',') : null
          }))
        })
      },
      { success: t('wms.receive.done'), then: reset }
    )
  } finally {
    saving.value = false
  }
}
</script>

<style lang="scss">
@import '../mobile.scss';
</style>
