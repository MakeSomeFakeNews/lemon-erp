<template>
  <view>
    <!-- 选择未完结的在途调拨单 -->
    <block v-if="!doc">
      <l-scan-input :placeholder="t('wms.transfer.scanDoc')" @scan="searchDocs" />
      <view v-for="d in docs" :key="d.id" class="m-card" @click="selectDoc(d.id)">
        <view class="m-row">
          <text class="m-title">{{ d.docNo }}</text>
          <l-tag :text="dictText(dict.wms_doc_status, d.status)" :color="dictColor(dict.wms_doc_status, d.status)" />
        </view>
        <view class="m-row m-sub">
          <text class="m-grow">{{ d.warehouseName }} → {{ d.toWarehouseName }}</text>
          <text class="m-nowrap">{{ d.bizDate }}</text>
        </view>
        <view class="m-sub">{{ t('wms.transfer.receivedOf', { received: qty(d.receivedQty), total: qty(d.totalQty) }) }}</view>
      </view>
      <l-empty v-if="loaded && !docs.length" :text="t('wms.transfer.noDocs')" />
    </block>

    <!-- 分批验收：扫码累计本次验收数量，可登记差异 -->
    <block v-else>
      <view class="m-card">
        <view class="m-row">
          <text class="m-title">{{ doc.docNo }}</text>
          <text class="m-link" @click="reset">{{ t('wms.transfer.changeDoc') }}</text>
        </view>
        <view class="m-sub">{{ doc.warehouseName }} → {{ doc.toWarehouseName }}</view>
        <view class="m-input">
          <text class="m-input__label">{{ t('wms.transfer.logisticsNo') }}</text>
          <input v-model="logisticsNo" maxlength="64" :placeholder="t('wms.transfer.optional')" placeholder-class="l-placeholder" />
        </view>
        <view class="m-input">
          <text class="m-input__label">{{ t('wms.transfer.plateNo') }}</text>
          <input v-model="plateNo" maxlength="32" :placeholder="t('wms.transfer.optional')" placeholder-class="l-placeholder" />
        </view>
        <view class="m-row m-actions">
          <text class="m-link" @click="fillAll">{{ t('wms.transfer.allArrived') }}</text>
          <text class="m-link" @click="clearAll">{{ t('wms.transfer.clearAll') }}</text>
        </view>
      </view>
      <l-scan-input :placeholder="t('wms.transfer.scanTip')" @scan="onScan" />
      <view v-if="tip.text" class="m-tip" :class="{ 'm-tip--fail': !tip.ok }">{{ tip.text }}</view>
      <view v-for="row in rows" :key="row.id" class="m-card" :class="{ 'm-card--active': row === activeRow }" @click="activeRow = row">
        <view class="m-row">
          <text class="m-title">{{ row.materialCode }} {{ row.materialName }}</text>
          <text :class="{ 'qty-done': used(row) >= row.remaining }">{{ qty(row.qty) }} / {{ qty(row.remaining) }} {{ row.unit }}</text>
        </view>
        <view v-if="row.spec || row.batchNo" class="m-sub">{{ row.spec }}{{ row.batchNo ? t('wms.common.batchN', { no: row.batchNo }) : '' }}</view>
        <view class="m-input" :class="{ 'm-input--disabled': row.serial }">
          <text class="m-input__label">{{ t('wms.transfer.receiveQty') }}</text>
          <input
            :value="row.qty"
            type="digit"
            :disabled="row.serial"
            @blur="(e) => (row.qty = toNumber(e.detail.value, 0, row.remaining) || 0)"
          />
        </view>
        <view v-if="row.serial" class="m-serials">
          <text>{{ t('wms.common.selectThenScanSerial') }}</text>
          <text v-for="s in row.serials" :key="s" class="m-serial" @click.stop="removeSerial(row, s)">{{ s }}</text>
        </view>
        <!-- 差异（序列号物料的差异要指定序列号，请在电脑端登记） -->
        <block v-if="!row.serial">
          <picker :range="diffTypes" range-key="label" :value="diffIndex(row)" @change="(e) => changeDiffType(row, e.detail.value)">
            <view class="m-input">
              <text class="m-input__label">{{ t('wms.transfer.diffType') }}</text>
              <text class="m-input__value" :class="{ 'l-placeholder': !row.diffType }">{{ diffTypes[diffIndex(row)].label }}</text>
            </view>
          </picker>
          <view v-if="row.diffType" class="m-input">
            <text class="m-input__label">{{ t('wms.transfer.diffQty') }}</text>
            <input :value="row.diffQty" type="digit" @blur="(e) => (row.diffQty = toNumber(e.detail.value, 0) || 0)" />
          </view>
          <view v-if="row.diffType" class="m-input">
            <text class="m-input__label">{{ t('wms.transfer.diffRemark') }}</text>
            <input v-model="row.diffRemark" maxlength="255" :placeholder="t('wms.transfer.optional')" placeholder-class="l-placeholder" />
          </view>
        </block>
        <view v-else class="m-sub">{{ t('wms.transfer.serialDiffOnWeb') }}</view>
        <view v-if="over(row)" class="m-tip m-tip--fail">{{ t('wms.transfer.exceeds') }}</view>
      </view>
      <view class="l-footer-space" />
      <view class="l-footer">
        <view class="m-footer-info">
          {{ t('wms.transfer.thisTime') }}<text class="m-strong">{{ qty(totalQty) }}</text>
          <text v-if="totalDiff">{{ t('wms.transfer.diffN', { n: qty(totalDiff) }) }}</text>
        </view>
        <button class="l-btn" :loading="saving" :disabled="saving || (!totalQty && !totalDiff)" @click="submit">
          {{ t('wms.transfer.submit') }}
        </button>
      </view>
    </block>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { TransferDiffType, TransitState } from '@web/constants/enums'
import { getTransfer, getTransferTransit, pageTransfer, receiveTransfer } from '@/api/wms'
import { useTitle } from '@/composables/useTitle'
import { dictColor, dictText, useDict } from '@/store/dict'
import { qty, today } from '@/utils/format'
import { qty4, resolveScan, toNumber, useScanTip } from '../mobile'

/**
 * 调拨验收（在途调拨的调拨入库）：选择未完结的在途调拨单，逐个扫描物料条码累计本次验收数量（包装条码按换算比例），
 * 序列号物料扫描调出时的序列号；非序列号物料可登记差异（少到、破损、错发、多发）。每次提交生成一张验收单，可分多次验收
 */
const { t } = useI18n()
useTitle('wms.entry.transferReceive')
const dict = useDict('wms_doc_status', 'wms_transfer_diff_type')
const tip = useScanTip()
const loaded = ref(false)
const docs = ref([])
const doc = ref(null)
const rows = ref([])
const activeRow = ref(null)
const logisticsNo = ref('')
const plateNo = ref('')
const saving = ref(false)

/** 差异类型：第一项为「无差异」；在途丢失由电脑端处理未到量生成，不可选 */
const diffTypes = computed(() => [
  { value: null, label: t('wms.transfer.noDiff') },
  ...(dict.wms_transfer_diff_type || []).filter((d) => d.value !== TransferDiffType.LOST)
])
const diffIndex = (row) => Math.max(0, diffTypes.value.findIndex((d) => d.value === (row.diffType || null)))
const isOver = (row) => row.diffType === TransferDiffType.OVER
const diffQty = (row) => (row.diffType ? Number(row.diffQty) || 0 : 0)
/** 占用未到量：验收 + 差异（多发除外） */
const used = (row) => qty4(Number(row.qty) + (isOver(row) ? 0 : diffQty(row)))
const over = (row) => used(row) > row.remaining
const totalQty = computed(() => qty4(rows.value.reduce((s, r) => s + (Number(r.qty) || 0), 0)))
const totalDiff = computed(() => qty4(rows.value.reduce((s, r) => s + diffQty(r), 0)))

onLoad(() => searchDocs())

async function searchDocs(docNo) {
  try {
    const res = await pageTransfer({ docNo: docNo || undefined, transitState: TransitState.PENDING, current: 1, size: 50 })
    docs.value = res.records || []
    if (docNo && docs.value.length === 1) selectDoc(docs.value[0].id)
  } finally {
    loaded.value = true
  }
}

async function selectDoc(id) {
  const [d, transit] = await Promise.all([getTransfer(id), getTransferTransit(id)])
  const serials = transit.availableSerials || {}
  const list = (d.items || [])
    .filter((i) => Number(i.remainingQty) > 0)
    .map((i) => ({
      id: i.id, materialId: i.materialId, materialCode: i.materialCode, materialName: i.materialName, spec: i.spec, unit: i.unit,
      batchNo: i.batchNo, serial: i.serialEnabled === 1, available: serials[i.id] || [], remaining: Number(i.remainingQty),
      qty: 0, serials: [], diffType: null, diffQty: 0, diffRemark: ''
    }))
  if (!list.length) return uni.showToast({ title: t('wms.transfer.noRemaining'), icon: 'none' })
  rows.value = list
  doc.value = d
  activeRow.value = null
  logisticsNo.value = ''
  plateNo.value = ''
  tip.text = ''
}

function reset() {
  doc.value = null
  searchDocs()
}

/** 全部到货：本次验收 = 未到量（序列号物料带出全部未到的序列号），清除差异 */
function fillAll() {
  rows.value.forEach((r) => {
    r.serials = r.serial ? [...r.available] : []
    r.qty = r.serial ? r.serials.length : r.remaining
    r.diffType = null
    r.diffQty = 0
  })
}

function clearAll() {
  rows.value.forEach((r) => {
    r.qty = 0
    r.serials = []
  })
}

function changeDiffType(row, index) {
  row.diffType = diffTypes.value[index]?.value || null
  row.diffQty = row.diffType && !isOver(row) ? Math.max(0, qty4(row.remaining - Number(row.qty))) : 0
}

async function onScan(code) {
  const hit = await resolveScan(code)
  if (hit.type === 'material') {
    const candidates = rows.value.filter((r) => r.materialId === hit.material.id)
    if (!candidates.length) return tip.show(t('wms.transfer.notInDoc', { code: hit.material.code }), false)
    const row = candidates.find((r) => used(r) < r.remaining) || candidates[0]
    activeRow.value = row
    if (row.serial) return tip.show(t('wms.common.scanSerial', { name: row.materialName }))
    if (qty4(used(row) + hit.ratio) > row.remaining) return tip.show(t('wms.transfer.overQty', { name: row.materialName }), false)
    row.qty = qty4(row.qty + hit.ratio)
    return tip.show(t('wms.transfer.added', { name: row.materialName, qty: qty(hit.ratio), total: qty(row.qty) }))
  }
  // 其他条码：作为序列号，须是调出时未到的序列号（先在该行查找，再在全部行中查找）
  const row = [activeRow.value, ...rows.value].find((r) => r?.serial && r.available.includes(code))
  if (!row) return tip.show(t('wms.transfer.serialNotInTransit', { code }), false)
  if (row.serials.includes(code)) return tip.show(t('wms.common.serialScanned', { code }), false)
  activeRow.value = row
  row.serials.push(code)
  row.qty = row.serials.length
  tip.show(t('wms.common.serialAdded', { name: row.materialName, code, n: row.qty }))
}

function removeSerial(row, s) {
  row.serials = row.serials.filter((x) => x !== s)
  row.qty = row.serials.length
}

async function submit() {
  const bad = rows.value.find(over)
  if (bad) return uni.showToast({ title: t('wms.transfer.lineExceeds', { name: bad.materialName }), icon: 'none' })
  const lines = rows.value
    .filter((r) => Number(r.qty) > 0 || diffQty(r) > 0)
    .map((r) => ({
      itemId: r.id,
      qty: Number(r.qty) || 0,
      serialNos: r.serial ? r.serials.join(',') || null : null,
      diffType: diffQty(r) > 0 ? r.diffType : null,
      diffQty: diffQty(r),
      diffRemark: diffQty(r) > 0 ? r.diffRemark || null : null
    }))
  saving.value = true
  try {
    await receiveTransfer(doc.value.id, {
      receiveDate: today(),
      logisticsNo: logisticsNo.value || null,
      plateNo: plateNo.value || null,
      lines
    })
    uni.showToast({ title: t('wms.transfer.success'), icon: 'success' })
    reset()
  } finally {
    saving.value = false
  }
}
</script>

<style lang="scss">
@import '../mobile.scss';

.m-actions {
  justify-content: flex-end;
  gap: 32rpx;
  margin-top: 16rpx;
}
</style>
