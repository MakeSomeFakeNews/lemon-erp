<template>
  <view>
    <!-- 选择盘点单 -->
    <block v-if="!check">
      <view class="m-tip check__hint">{{ t('wms.check.tip') }}</view>
      <view v-for="c in checks" :key="c.id" class="m-card" @click="selectCheck(c.id)">
        <view class="m-row">
          <text class="m-title">{{ c.docNo }}</text>
          <text class="m-sub">{{ c.bizDate }}</text>
        </view>
        <view class="m-sub">{{ c.warehouseName }}{{ c.remark ? ` · ${c.remark}` : '' }}</view>
      </view>
      <l-empty v-if="loaded && !checks.length" :text="t('wms.check.noChecks')" />
    </block>

    <!-- 按库位扫码盘点 -->
    <block v-else>
      <view class="m-card">
        <view class="m-row">
          <text class="m-title">{{ check.docNo }}</text>
          <text class="m-link" @click="leave">{{ t('wms.putaway.change') }}</text>
        </view>
        <view class="m-sub">{{ check.warehouseName }} · {{ t('wms.check.counted', { n: countedCount, total: items.length }) }}</view>
      </view>
      <l-scan-input :placeholder="t('wms.common.scanLocThenItem')" @scan="onScan" />
      <view v-if="tip.text" class="m-tip" :class="{ 'm-tip--fail': !tip.ok }">{{ tip.text }}</view>
      <view v-if="currentLocation" class="m-current">
        <text>{{ t('wms.common.currentLocation') }}</text>
        <l-tag :text="currentLocation.code" color="blue" />
        <text class="check__only">{{ t('wms.check.onlyCurrent') }}</text>
        <switch :checked="onlyCurrent" style="transform: scale(0.7)" @change="(e) => (onlyCurrent = e.detail.value)" />
      </view>
      <view v-for="row in shown" :key="row.key" class="m-card" :class="{ 'm-card--active': row === activeRow }" @click="activeRow = row">
        <view class="m-row">
          <l-tag :text="locationText(row.locationCode)" color="orange" />
          <text class="m-sub">{{ t('wms.check.book', { n: qty(row.bookQty) }) }} {{ row.unit }}</text>
        </view>
        <view class="m-row check__name">
          <text class="m-title">{{ row.materialCode }} {{ row.materialName }}</text>
          <l-tag v-if="row.added" :text="t('wms.check.gain')" color="green" />
        </view>
        <view class="m-sub">{{ row.spec || '' }}{{ row.batchNo && !row.added ? t('wms.common.batchN', { no: row.batchNo }) : '' }}</view>
        <view v-if="row.added && row.batchEnabled === 1" class="m-input">
          <text class="m-input__label">{{ t('wms.common.batch') }}</text>
          <input v-model="row.batchNo" maxlength="64" :placeholder="t('common.pleaseInput')" placeholder-class="l-placeholder" />
        </view>
        <view v-if="row.serialEnabled !== 1" class="m-input">
          <text class="m-input__label">{{ t('wms.check.actual') }}</text>
          <input :value="row.actualQty" type="digit" :class="diffClass(row)" @blur="(e) => (row.actualQty = toNumber(e.detail.value, 0))" />
        </view>
        <view v-else class="m-serials">
          <text>{{ t('wms.check.actualScan', { n: row.serials.length }) }}</text>
          <text v-for="s in row.serials" :key="s" class="m-serial" @click.stop="removeSerial(row, s)">{{ s }}</text>
        </view>
      </view>
      <view class="l-footer-space" />
      <view class="l-footer">
        <view class="m-footer-info">
          {{ t('wms.check.diff') }}<text class="m-strong">{{ diffCount }}</text>
        </view>
        <button class="l-btn" :loading="saving" :disabled="saving" @click="save">{{ t('wms.check.save') }}</button>
      </view>
    </block>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { WmsDocStatus } from '@web/constants/enums'
import { getCheck, locationOptions, pageCheck, updateCheck } from '@/api/wms'
import { useTitle } from '@/composables/useTitle'
import { runAction } from '@/composables/useAction'
import { qty } from '@/utils/format'
import { locationText, qty4, resolveScan, rowKey, splitSerials, toNumber, useScanTip } from '../mobile'

/**
 * 盘点：选择草稿盘点单，扫描库位后扫描物料累计实盘数量（包装条码按换算比例），序列号物料扫描序列号；
 * 账面没有的物料作为盘盈行添加。保存实盘数量，盘点确认仍在后台进行
 */
const { t } = useI18n()
useTitle('wms.entry.check')
const tip = useScanTip()
const loaded = ref(false)
const checks = ref([])
const check = ref(null)
const items = ref([])
const locations = ref([])
const currentLocation = ref(null)
const onlyCurrent = ref(true)
const activeRow = ref(null)
const saving = ref(false)

onLoad(async () => {
  try {
    checks.value = (await pageCheck({ status: WmsDocStatus.DRAFT, current: 1, size: 50 })).records || []
  } finally {
    loaded.value = true
  }
})

async function selectCheck(id) {
  const c = await getCheck(id)
  items.value = (c.items || []).map((i) => ({
    ...i, key: `item-${i.id}`, bookQty: Number(i.bookQty), actualQty: i.actualQty == null ? null : Number(i.actualQty),
    serials: splitSerials(i.serialNos), added: false
  }))
  locations.value = await locationOptions(c.warehouseId)
  check.value = c
  currentLocation.value = null
  activeRow.value = null
  tip.text = ''
}

function leave() {
  check.value = null
}

const shown = computed(() =>
  currentLocation.value && onlyCurrent.value ? items.value.filter((r) => r.locationId === currentLocation.value.id) : items.value
)
const actualOf = (r) => (r.serialEnabled === 1 ? (r.serials.length || r.actualQty != null ? r.serials.length : null) : r.actualQty)
const countedCount = computed(() => items.value.filter((r) => actualOf(r) != null).length)
const diffCount = computed(() => items.value.filter((r) => actualOf(r) != null && qty4(actualOf(r)) !== r.bookQty).length)
const diffClass = (r) => (r.actualQty == null ? '' : qty4(r.actualQty) === r.bookQty ? 'qty-done' : 'qty-short')

async function onScan(code) {
  const hit = await resolveScan(code, locations.value)
  if (hit.type === 'location') {
    currentLocation.value = hit.location
    activeRow.value = null
    return tip.show(t('wms.check.locationLines', { code: hit.location.code, n: shown.value.length }))
  }
  if (hit.type === 'material') {
    const locationId = currentLocation.value?.id || null
    const list = items.value.filter((r) => r.materialId === hit.material.id && (r.locationId || null) === locationId)
    let row = list.find((r) => r === activeRow.value) || list[0]
    if (!row) {
      // 账面没有：盘盈
      const m = hit.material
      row = {
        key: rowKey(), materialId: m.id, materialCode: m.code, materialName: m.name, spec: m.spec, unit: m.unit, batchEnabled: m.batchEnabled,
        serialEnabled: m.serialEnabled, locationId, locationCode: currentLocation.value?.code, batchNo: '', bookQty: 0, actualQty: null, serials: [], added: true
      }
      items.value.push(row)
      row = items.value[items.value.length - 1]
    }
    activeRow.value = row
    if (row.serialEnabled === 1) return tip.show(t('wms.common.scanSerial', { name: row.materialName }))
    row.actualQty = qty4((row.actualQty || 0) + hit.ratio)
    return tip.show(t(row.added ? 'wms.check.addedGain' : 'wms.check.added', { name: row.materialName, qty: qty(hit.ratio), actual: qty(row.actualQty) }))
  }
  const row = activeRow.value
  if (row?.serialEnabled !== 1) return tip.show(t('wms.common.unknownCode', { code }), false)
  if (items.value.some((r) => r.serials.includes(code))) return tip.show(t('wms.common.serialScanned', { code }), false)
  row.serials.push(code)
  tip.show(t('wms.check.serialAdded', { name: row.materialName, code, n: row.serials.length }))
}

function removeSerial(row, s) {
  row.serials = row.serials.filter((x) => x !== s)
}

async function save() {
  const noBatch = items.value.find((r) => r.added && r.batchEnabled === 1 && !r.batchNo)
  if (noBatch) return uni.showToast({ title: t('wms.check.inputGainBatch', { name: noBatch.materialName }), icon: 'none' })
  const c = check.value
  saving.value = true
  try {
    await runAction(
      () =>
        updateCheck(c.id, {
          warehouseId: c.warehouseId,
          bizDate: c.bizDate,
          remark: c.remark,
          freeze: c.freeze === true || c.freeze === 1,
          items: items.value
            .filter((r) => !r.added || actualOf(r) != null)
            .map((r) => ({
              materialId: r.materialId, locationId: r.locationId || null, batchNo: r.batchNo || null, actualQty: actualOf(r),
              serialNos: r.serialEnabled === 1 && r.serials.length ? r.serials.join(',') : null, remark: r.remark || null
            }))
        }),
      { success: t('wms.check.saved'), then: () => selectCheck(c.id) }
    )
  } finally {
    saving.value = false
  }
}
</script>

<style lang="scss">
@import '../mobile.scss';

.check {
  &__hint {
    margin: 24rpx;
    color: $l-text-3;
  }
  &__name {
    margin-top: 12rpx;
  }
  &__only {
    margin-left: auto;
  }
}
</style>
