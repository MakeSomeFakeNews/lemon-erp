<template>
  <view>
    <!-- 选择发货通知 -->
    <block v-if="!detail">
      <l-scan-input :placeholder="t('wms.pick.scanDelivery')" @scan="searchDeliveries" />
      <view v-for="d in deliveries" :key="d.id" class="m-card" @click="selectDelivery(d.id)">
        <view class="m-row">
          <text class="m-title">{{ d.deliveryNo }}</text>
          <l-tag v-if="d.waveNo" :text="d.waveNo" color="blue" />
        </view>
        <view class="m-row m-sub">
          <text>{{ d.customerName }} · {{ d.warehouseName }}</text>
          <text>{{ d.shipDate }}</text>
        </view>
      </view>
      <l-empty v-if="loaded && !deliveries.length" :text="t('wms.pick.noDeliveries')" />
    </block>

    <!-- 按库位顺序拣货 -->
    <block v-else>
      <view class="m-card">
        <view class="m-row">
          <text class="m-title">{{ detail.deliveryNo }}</text>
          <text class="m-link" @click="reset">{{ t('wms.putaway.change') }}</text>
        </view>
        <view class="m-sub">{{ detail.customerName }} · {{ detail.warehouseName }}</view>
        <view v-if="shortItems.length" class="pick__warn">
          {{ t('wms.pick.shortage', { names: shortItems.map((i) => i.materialName).join(t('wms.common.listSep')) }) }}
        </view>
      </view>
      <l-scan-input :placeholder="t('wms.common.scanLocThenItem')" @scan="onScan" />
      <view v-if="tip.text" class="m-tip" :class="{ 'm-tip--fail': !tip.ok }">{{ tip.text }}</view>
      <view v-if="currentLocation" class="m-current">
        <text>{{ t('wms.common.currentLocation') }}</text>
        <l-tag :text="currentLocation.code" color="blue" />
      </view>
      <view v-for="row in rows" :key="row.key" class="m-card" :class="{ 'm-card--active': row === activeRow }" @click="activeRow = row">
        <view class="m-row">
          <l-tag :text="locationText(row.locationCode)" color="orange" />
          <text :class="row.picked >= row.qty ? 'qty-done' : row.picked > 0 ? 'qty-short' : ''">{{ qty(row.picked) }} / {{ qty(row.qty) }} {{ row.unit }}</text>
        </view>
        <view class="m-title pick__name">{{ row.materialCode }} {{ row.materialName }}</view>
        <view class="m-sub">{{ row.spec || '' }}{{ row.batchNo ? t('wms.common.batchN', { no: row.batchNo }) : '' }}</view>
        <view v-if="row.serialEnabled !== 1" class="m-input">
          <text class="m-input__label">{{ t('wms.pick.picked') }}</text>
          <input :value="row.picked" type="digit" @blur="(e) => (row.picked = toNumber(e.detail.value, 0, row.qty) || 0)" />
        </view>
        <view v-else class="m-serials">
          <text>{{ t('wms.common.scanSerialOnly') }}</text>
          <text v-for="s in row.serials" :key="s" class="m-serial" @click.stop="removeSerial(row, s)">{{ s }}</text>
        </view>
      </view>
      <view class="l-footer-space" />
      <view class="l-footer">
        <view class="m-footer-info">
          {{ t('wms.pick.picked') }}<text class="m-strong">{{ doneCount }}</text>/ {{ t('wms.common.lines', { n: rows.length }) }}
        </view>
        <button class="l-btn" :loading="saving" :disabled="saving || !!shortItems.length" @click="submit">{{ t('wms.pick.submit') }}</button>
      </view>
    </block>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { DeliveryStatus } from '@web/constants/enums'
import { availableSerials, getDelivery, locationOptions, pageDelivery, pickDelivery } from '@/api/wms'
import { useTitle } from '@/composables/useTitle'
import { runAction } from '@/composables/useAction'
import { qty } from '@/utils/format'
import { locationText, qty4, resolveScan, toNumber, useScanTip } from '../mobile'

/**
 * 拣货：按拣货建议（先到期先出）以库位顺序逐行拣货，扫描库位后扫描物料累计数量，序列号物料扫描序列号；
 * 全部拣完后确认拣货，之后在后台出库
 */
const { t } = useI18n()
useTitle('wms.entry.pick')
const tip = useScanTip()
const loaded = ref(false)
const deliveries = ref([])
const detail = ref(null)
const locations = ref([])
const currentLocation = ref(null)
const rows = ref([])
const activeRow = ref(null)
const saving = ref(false)

onLoad(() => searchDeliveries())

async function searchDeliveries(keyword) {
  try {
    const res = await pageDelivery({ status: DeliveryStatus.PENDING, keyword: keyword || undefined, current: 1, size: 50 })
    deliveries.value = res.records || []
    if (keyword && deliveries.value.length === 1) selectDelivery(deliveries.value[0].id)
  } finally {
    loaded.value = true
  }
}

async function selectDelivery(id) {
  const d = await getDelivery(id)
  const items = new Map((d.items || []).map((i) => [i.id, i]))
  rows.value = (d.picks || [])
    .map((p) => {
      const item = items.get(p.deliveryItemId) || {}
      return {
        key: p.id, deliveryItemId: p.deliveryItemId, materialId: item.materialId, materialCode: item.materialCode, materialName: item.materialName,
        spec: item.spec, unit: item.unit, serialEnabled: item.serialEnabled, locationId: p.locationId, locationCode: p.locationCode,
        batchNo: p.batchNo, qty: Number(p.qty), picked: 0, serials: [], serialPool: null
      }
    })
    .sort((a, b) => String(a.locationCode || '').localeCompare(String(b.locationCode || '')) || a.deliveryItemId - b.deliveryItemId)
  detail.value = d
  currentLocation.value = null
  activeRow.value = null
  tip.text = ''
  locations.value = await locationOptions(d.warehouseId)
}

function reset() {
  detail.value = null
  searchDeliveries()
}

/** 拣货建议合计少于通知数量的明细（库存不足） */
const shortItems = computed(() =>
  (detail.value?.items || []).filter((i) => qty4(rows.value.filter((r) => r.deliveryItemId === i.id).reduce((s, r) => s + r.qty, 0)) < Number(i.qty))
)
const doneCount = computed(() => rows.value.filter((r) => r.picked >= r.qty).length)

async function onScan(code) {
  const hit = await resolveScan(code, locations.value)
  if (hit.type === 'location') {
    currentLocation.value = hit.location
    const next = rows.value.find((r) => r.locationId === hit.location.id && r.picked < r.qty)
    if (!next) return tip.show(t('wms.pick.nothingHere', { code: hit.location.code }), false)
    activeRow.value = next
    return tip.show(t('wms.common.locationN', { code: hit.location.code }))
  }
  if (hit.type === 'material') {
    const list = rows.value.filter((r) => r.materialId === hit.material.id && r.picked < r.qty)
    if (!list.length) return tip.show(t('wms.pick.notNeeded', { code: hit.material.code }), false)
    const row = currentLocation.value ? list.find((r) => r.locationId === currentLocation.value.id) : list.find((r) => !r.locationId)
    if (!row) return tip.show(t('wms.pick.goTo', { locations: list.map((r) => locationText(r.locationCode)).join(t('wms.common.listSep')) }), false)
    activeRow.value = row
    if (row.serialEnabled === 1) return tip.show(t('wms.common.scanSerial', { name: row.materialName }))
    if (qty4(row.picked + hit.ratio) > row.qty) return tip.show(t('wms.pick.overQty', { name: row.materialName }), false)
    row.picked = qty4(row.picked + hit.ratio)
    return tip.show(t('wms.pick.added', { name: row.materialName, qty: qty(hit.ratio), picked: qty(row.picked), total: qty(row.qty) }))
  }
  const row = activeRow.value
  if (row?.serialEnabled !== 1) return tip.show(t('wms.common.unknownCode', { code }), false)
  if (!row.serialPool) {
    row.serialPool = await availableSerials({ warehouseId: detail.value.warehouseId, locationId: row.locationId || undefined, materialId: row.materialId,
      batchNo: row.batchNo || undefined })
  }
  if (!row.serialPool.includes(code)) return tip.show(t('wms.pick.serialNotHere', { code }), false)
  if (rows.value.some((r) => r.serials.includes(code))) return tip.show(t('wms.common.serialScanned', { code }), false)
  if (row.serials.length >= row.qty) return tip.show(t('wms.pick.overQty', { name: row.materialName }), false)
  row.serials.push(code)
  row.picked = row.serials.length
  tip.show(t('wms.pick.serialAdded', { name: row.materialName, code, picked: row.picked, total: row.qty }))
}

function removeSerial(row, s) {
  row.serials = row.serials.filter((x) => x !== s)
  row.picked = row.serials.length
}

async function submit() {
  const left = rows.value.find((r) => qty4(Number(r.picked) || 0) !== r.qty)
  if (left) {
    const msg = t('wms.pick.unfinished', { location: locationText(left.locationCode), name: left.materialName, picked: qty(left.picked), total: qty(left.qty) })
    return uni.showToast({ title: msg, icon: 'none', duration: 3000 })
  }
  saving.value = true
  try {
    await runAction(
      () =>
        pickDelivery(detail.value.id, {
          picks: rows.value.map((r) => ({
            deliveryItemId: r.deliveryItemId, locationId: r.locationId || null, batchNo: r.batchNo || null, qty: r.qty,
            serialNos: r.serialEnabled === 1 ? r.serials.join(',') : null
          }))
        }),
      { success: t('wms.pick.done'), then: reset }
    )
  } finally {
    saving.value = false
  }
}
</script>

<style lang="scss">
@import '../mobile.scss';

.pick {
  &__warn {
    margin-top: 16rpx;
    padding: 16rpx 20rpx;
    border-radius: 12rpx;
    font-size: 24rpx;
    color: #d46b08;
    background: #fff7e6;
  }
  &__name {
    margin-top: 12rpx;
  }
}
</style>
