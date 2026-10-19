<template>
  <view>
    <view class="m-card">
      <picker :range="warehouses" range-key="name" :value="warehouseIndex" @change="changeWarehouse">
        <view class="m-input">
          <text class="m-input__label">{{ t('wms.common.warehouse') }}</text>
          <text class="m-input__value" :class="{ 'l-placeholder': !warehouseId }">{{ warehouseName || t('common.pleaseSelect') }}</text>
        </view>
      </picker>
      <view v-if="source" class="m-row m-sub">
        <view>
          <text>{{ t('wms.putaway.source') }} </text>
          <l-tag :text="source.code" color="orange" />
        </view>
        <text class="m-link" @click="clearSource">{{ t('wms.putaway.change') }}</text>
      </view>
    </view>
    <l-scan-input :placeholder="source ? t('wms.putaway.scanItem') : t('wms.putaway.scanSource')" @scan="onScan" />
    <view v-if="tip.text" class="m-tip" :class="{ 'm-tip--fail': !tip.ok }">{{ tip.text }}</view>
    <view v-for="row in rows" :key="row.key" class="m-card" :class="{ 'm-card--active': row === activeRow }" @click="activeRow = row">
      <view class="m-row">
        <text class="m-title">{{ row.materialCode }} {{ row.materialName }}</text>
        <text class="m-sub">{{ t('wms.common.availableN', { n: qty(row.available) }) }} {{ row.unit }}</text>
      </view>
      <view class="m-sub">{{ row.spec || '' }}{{ row.batchNo ? t('wms.common.batchN', { no: row.batchNo }) : '' }}</view>
      <view class="m-input" :class="{ 'm-input--disabled': row.serialEnabled === 1 }">
        <text class="m-input__label">{{ t('wms.putaway.qty') }}</text>
        <input
          :value="row.qty"
          type="digit"
          :disabled="row.serialEnabled === 1"
          @blur="(e) => (row.qty = toNumber(e.detail.value, 0, row.available) || 0)"
        />
      </view>
      <picker :range="locRange" range-key="label" :value="locationIndex(locRange, row.toLocationId)" @change="(e) => (row.toLocationId = locRange[e.detail.value].id)">
        <view class="m-input">
          <text class="m-input__label">{{ t('wms.putaway.target') }}</text>
          <text class="m-input__value" :class="{ 'l-placeholder': !row.toLocationId }">{{ locRange[locationIndex(locRange, row.toLocationId)].label }}</text>
        </view>
      </picker>
      <view v-if="row.serialEnabled === 1" class="m-serials">
        <text>{{ t('wms.common.selectThenScanSerial') }}</text>
        <text v-for="s in row.serials" :key="s" class="m-serial" @click.stop="removeSerial(row, s)">{{ s }}</text>
      </view>
    </view>
    <l-empty v-if="source && loaded && !rows.length" :text="t('wms.putaway.noStock')" />
    <block v-if="rows.length">
      <view class="l-footer-space" />
      <view class="l-footer">
        <button class="l-btn l-btn--ghost" @click="suggest">{{ t('wms.putaway.suggest') }}</button>
        <button class="l-btn" :loading="saving" :disabled="saving" @click="submit">{{ t('wms.putaway.submit', { n: moves.length }) }}</button>
      </view>
    </block>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { addDoc, availableSerials, confirmDoc, locationOptions, pageStock, warehouseOptions } from '@/api/wms'
import { useTitle } from '@/composables/useTitle'
import { runAction } from '@/composables/useAction'
import { hasPerm } from '@/store/user'
import { qty, today } from '@/utils/format'
import { availableQty, fillPutaway, locationIndex, locationRange, resolveScan, toNumber, useScanTip } from '../mobile'

/**
 * 上架：扫描来源库位（收货区等）列出库存，扫描物料选中后扫描目标库位，或按上架策略推荐；
 * 生成同仓库的调拨单并确认（没有调拨确认权限时保存为草稿）
 */
const { t } = useI18n()
useTitle('wms.entry.putaway')
const tip = useScanTip()
const warehouses = ref([])
const warehouseId = ref(null)
const locations = ref([])
const source = ref(null)
const rows = ref([])
const activeRow = ref(null)
const loaded = ref(false)
const saving = ref(false)

const warehouseIndex = computed(() => Math.max(0, warehouses.value.findIndex((w) => w.id === warehouseId.value)))
const warehouseName = computed(() => warehouses.value.find((w) => w.id === warehouseId.value)?.name)
const locRange = computed(() => locationRange(locations.value, t('common.pleaseSelect')))
const moves = computed(() => rows.value.filter((r) => Number(r.qty) > 0 && r.toLocationId))

onLoad(() => {
  warehouseOptions().then((list) => (warehouses.value = list || [])).catch(() => {})
})

async function changeWarehouse(e) {
  warehouseId.value = warehouses.value[e.detail.value]?.id || null
  clearSource()
  locations.value = warehouseId.value ? await locationOptions(warehouseId.value) : []
}

function clearSource() {
  source.value = null
  rows.value = []
  activeRow.value = null
  loaded.value = false
}

async function loadStock() {
  try {
    const res = await pageStock({ warehouseId: warehouseId.value, locationId: source.value.id, current: 1, size: 500 })
    rows.value = (res.records || [])
      .filter((s) => availableQty(s) > 0)
      .map((s) => ({
        key: s.id, materialId: s.materialId, materialCode: s.materialCode, materialName: s.materialName, spec: s.spec, unit: s.unit,
        serialEnabled: s.serialEnabled, batchNo: s.batchNo, available: availableQty(s), qty: s.serialEnabled === 1 ? 0 : availableQty(s),
        toLocationId: null, serials: [], serialPool: null
      }))
  } finally {
    loaded.value = true
  }
}

async function onScan(code) {
  if (!warehouseId.value) return tip.show(t('wms.common.selectWarehouseFirst'), false)
  const hit = await resolveScan(code, locations.value)
  if (hit.type === 'location') {
    if (!source.value) {
      source.value = hit.location
      await loadStock()
      return tip.show(t('wms.putaway.sourceN', { code: hit.location.code, n: rows.value.length }))
    }
    if (!activeRow.value) return tip.show(t('wms.putaway.scanItemFirst'), false)
    if (hit.location.id === source.value.id) return tip.show(t('wms.putaway.sameLocation'), false)
    activeRow.value.toLocationId = hit.location.id
    return tip.show(`${activeRow.value.materialName} → ${hit.location.code}`)
  }
  if (!source.value) return tip.show(t('wms.putaway.scanSourceFirst'), false)
  if (hit.type === 'material') {
    const row = rows.value.find((r) => r.materialId === hit.material.id && !r.toLocationId) || rows.value.find((r) => r.materialId === hit.material.id)
    if (!row) return tip.show(t('wms.common.notInLocation', { code: hit.material.code }), false)
    activeRow.value = row
    return tip.show(row.serialEnabled === 1 ? t('wms.common.scanSerial', { name: row.materialName }) : t('wms.putaway.selected', { name: row.materialName }))
  }
  const row = activeRow.value
  if (row?.serialEnabled !== 1) return tip.show(t('wms.common.unknownCode', { code }), false)
  if (!row.serialPool) {
    row.serialPool = await availableSerials({ warehouseId: warehouseId.value, locationId: source.value.id, materialId: row.materialId, batchNo: row.batchNo || undefined })
  }
  if (!row.serialPool.includes(code)) return tip.show(t('wms.putaway.serialNotHere', { code }), false)
  if (row.serials.includes(code)) return tip.show(t('wms.common.serialScanned', { code }), false)
  row.serials.push(code)
  row.qty = row.serials.length
  tip.show(t('wms.common.serialAdded', { name: row.materialName, code, n: row.qty }))
}

function removeSerial(row, s) {
  row.serials = row.serials.filter((x) => x !== s)
  row.qty = row.serials.length
}

async function suggest() {
  const targets = rows.value.filter((r) => Number(r.qty) > 0 && !r.toLocationId)
  await fillPutaway(warehouseId.value, targets, 'toLocationId')
  // 推荐结果可能就是来源库位（如默认库位就是收货区），不上架
  targets.forEach((r) => r.toLocationId === source.value.id && (r.toLocationId = null))
  const filled = targets.filter((r) => r.toLocationId).length
  uni.showToast({ title: filled ? t('wms.putaway.suggested', { n: filled }) : t('wms.putaway.noSuggestion'), icon: 'none' })
}

async function submit() {
  if (!moves.value.length) return uni.showToast({ title: t('wms.putaway.fillMoves'), icon: 'none' })
  const canConfirm = hasPerm('wms:transfer:confirm')
  saving.value = true
  try {
    await runAction(
      async () => {
        const id = await addDoc('transfer', {
          warehouseId: warehouseId.value,
          toWarehouseId: warehouseId.value,
          bizDate: today(),
          remark: '移动上架', // i18n-ignore 存入单据备注（与 Web 端一致）
          items: moves.value.map((r) => ({
            materialId: r.materialId, locationId: source.value.id, toLocationId: r.toLocationId, batchNo: r.batchNo || null, qty: r.qty,
            serialNos: r.serialEnabled === 1 ? r.serials.join(',') : null
          }))
        })
        if (canConfirm) await confirmDoc('transfer', id)
      },
      { success: canConfirm ? t('wms.putaway.done') : t('wms.putaway.pending'), then: loadStock }
    )
  } finally {
    saving.value = false
  }
}
</script>

<style lang="scss">
@import '../mobile.scss';
</style>
