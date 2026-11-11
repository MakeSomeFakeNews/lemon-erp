<template>
  <view>
    <l-empty v-if="disabled" :text="t('trade.purchase.disabled')" />
    <block v-else>
      <!-- 表头：供应商、仓库、发票类型、日期 -->
      <view class="m-card">
        <view class="m-input" @click="openSheet('supplier')">
          <text class="m-input__label">{{ t('trade.purchase.supplier') }}</text>
          <text class="m-input__value" :class="{ 'l-placeholder': !supplier }">{{ supplier ? `${supplier.code} ${supplier.name}` : t('common.pleaseSelect') }}</text>
        </view>
        <picker :range="warehouses" range-key="name" :value="warehouseIndex" @change="changeWarehouse">
          <view class="m-input">
            <text class="m-input__label">{{ t('wms.common.warehouse') }}</text>
            <text class="m-input__value" :class="{ 'l-placeholder': !warehouseId }">{{ warehouseName || t('common.pleaseSelect') }}</text>
          </view>
        </picker>
        <!-- 发票类型（业务字典 scm_invoice_kind）：选择后明细税率统一调整为该税率，可逐行再改 -->
        <picker :range="invoiceKindChoices" range-key="label" :value="invoiceKindIndex" @change="changeInvoiceKind">
          <view class="m-input">
            <text class="m-input__label">{{ t('wms.sale.invoiceKind') }}</text>
            <text class="m-input__value" :class="{ 'l-placeholder': !invoiceKind }">{{ invoiceKindChoices[invoiceKindIndex].label }}</text>
          </view>
        </picker>
        <picker mode="date" :value="bizDate" @change="changeDate">
          <view class="m-input">
            <text class="m-input__label">{{ t('trade.purchase.bizDate') }}</text>
            <text class="m-input__value">{{ bizDate }}</text>
          </view>
        </picker>
      </view>

      <!-- 商品：扫码（物料条码、包装条码、库位条码、序列号）或搜索 -->
      <view class="pur__scan">
        <view class="pur__scan-input"><l-scan-input :placeholder="t('trade.purchase.scanTip')" @scan="onScan" /></view>
        <text class="m-link pur__search" @click="openSheet('material')">{{ t('wms.sale.searchMaterial') }}</text>
      </view>
      <view v-if="tip.text" class="m-tip" :class="{ 'm-tip--fail': !tip.ok }">{{ tip.text }}</view>
      <view v-if="currentLocation" class="m-current">
        <text>{{ t('wms.common.currentLocation') }}</text>
        <l-tag :text="currentLocation.code" color="blue" />
        <text class="m-link" @click="currentLocation = null">{{ t('wms.common.clear') }}</text>
      </view>

      <PurchaseRow
        v-for="row in rows"
        :key="row.key"
        :row="row"
        :active="row === activeRow"
        :locations="locRange"
        @activate="activeRow = row"
        @remove="removeRow(row)"
        @remove-serial="(s) => removeSerial(row, s)"
        @changed="fillPrices([row])"
      />
      <l-empty v-if="!rows.length" :text="t('trade.purchase.emptyRows')" />

      <view v-if="rows.length" class="m-card">
        <view class="m-row pur__sum">
          <text>{{ t('wms.sale.total') }}</text>
          <text>{{ money(totalAmount) }}</text>
        </view>
        <view class="m-input">
          <text class="m-input__label">{{ t('common.remark') }}</text>
          <input v-model="remark" maxlength="255" :placeholder="t('wms.sale.optional')" placeholder-class="l-placeholder" />
        </view>
      </view>

      <view class="l-footer-space" />
      <view class="l-footer">
        <view class="m-footer-info">
          {{ t('trade.purchase.payable') }}<text class="m-strong">{{ money(totalAmount) }}</text>
        </view>
        <button
          v-for="m in modes"
          :key="m.mode"
          class="l-btn pur__save"
          :class="{ 'l-btn--ghost': m.ghost }"
          :loading="saving === m.mode"
          :disabled="!!saving || !rows.length"
          @click="submit(m.mode)"
        >
          {{ m.mode === 'post' && modes.length === 1 ? t('trade.purchase.submit') : m.label }}
        </button>
      </view>
    </block>

    <SearchSheet
      :open="!!sheet"
      :title="sheet ? t(`trade.purchase.sheet.${sheet}`) : ''"
      :items="sheetItems"
      :loading="sheetLoading"
      :has-more="sheetHasMore"
      @search="searchSheet"
      @more="searchSheet(sheetKeyword, true)"
      @select="selectSheet"
      @close="sheet = ''"
    />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { OPTION_PAGE_SIZE, defaultPrices, locationOptions, materialOptions, warehouseOptions } from '@/api/wms'
import { supplierOptions } from '@/api/trade'
import { useTitle } from '@/composables/useTitle'
import { bizRule, loadBizRules } from '@/store/bizRules'
import { useDict } from '@/store/dict'
import { ensureLogin } from '@/utils/session'
import { money, qty, today } from '@/utils/format'
import { fillPutaway, locationRange, qty4, resolveScan, rowKey, useScanTip } from '../mobile'
import { baseQty, basePrice, lineAmount, loadUnits, ratioOf } from '../sale/lines'
import { saveModes, saveTrade } from '../tradeSave'
import PurchaseRow from './PurchaseRow.vue'
import SearchSheet from '../components/SearchSheet.vue'

/**
 * 快速入库：采购入库直接开单（不引用采购订单，与 Web 端采购入库「直接开单」相同的接口 POST /scm/purchaseIn）。
 * 选供应商、仓库，扫码或搜索商品，含税单价按 /scm/pricing/default（合同价 → 供货价 → 最近进价 → 参考成本价）带出；
 * 物料有辅助单位时可选录入单位（扫包装条码默认该单位），提交时折算为基本单位；批次物料必须填批次号，序列号物料扫描序列号；
 * 库位可选（扫库位条码设为当前库位），不选的按上架策略推荐。业务参数 scm.directDoc.enabled 关闭时不可用
 */
const { t } = useI18n()
useTitle('trade.entry.purchase')
const tip = useScanTip()
const WAREHOUSE_KEY = 'lemon_mp_purchase_warehouse'
const KIND = 'purchaseIn'

const disabled = ref(false)
const modes = ref(saveModes(KIND))
const supplier = ref(null)
const warehouses = ref([])
const warehouseId = ref(null)
const locations = ref([])
const currentLocation = ref(null)
const bizDate = ref(today())
const remark = ref('')
const rows = ref([])
const activeRow = ref(null)
const saving = ref('')

const warehouseIndex = computed(() => Math.max(0, warehouses.value.findIndex((w) => w.id === warehouseId.value)))
const warehouseName = computed(() => warehouses.value.find((w) => w.id === warehouseId.value)?.name)
const locRange = computed(() => locationRange(locations.value, t('wms.receive.locationPh')))
const totalAmount = computed(() => Math.round(rows.value.reduce((s, r) => s + lineAmount(r), 0) * 100) / 100)
const defaultTaxRate = computed(() => {
  const v = bizRule('scm.defaultTaxRate', null)
  return v == null || v === '' ? null : Number(v)
})

// 发票类型（与快速开单一致）
const dict = useDict('scm_invoice_kind')
const invoiceKind = ref(null)
const kindRate = (d) => (d && d.extValue != null && d.extValue !== '' ? Number(d.extValue) : null)
const invoiceKindChoices = computed(() => [
  { value: null, label: t('wms.sale.invoiceKindNone') },
  ...dict.scm_invoice_kind.map((d) => ({ value: d.value, label: kindRate(d) != null ? `${d.label}（${kindRate(d)}%）` : d.label }))
])
const invoiceKindIndex = computed(() => Math.max(0, invoiceKindChoices.value.findIndex((k) => k.value === invoiceKind.value)))
const newRowTaxRate = () => kindRate(dict.scm_invoice_kind.find((d) => d.value === invoiceKind.value)) ?? defaultTaxRate.value
function changeInvoiceKind(e) {
  invoiceKind.value = invoiceKindChoices.value[Number(e.detail.value)].value
  const rate = kindRate(dict.scm_invoice_kind.find((d) => d.value === invoiceKind.value))
  if (rate == null) return
  rows.value.forEach((r) => (r.taxRate = rate))
  if (rows.value.length) uni.showToast({ title: t('wms.sale.invoiceKindApplied', { n: rows.value.length, rate }), icon: 'none' })
}

onLoad(async () => {
  if (!(await ensureLogin())) return
  await loadBizRules(['scm.directDoc.enabled', 'scm.tradeDoc.draft', 'scm.defaultTaxRate'])
  disabled.value = bizRule('scm.directDoc.enabled', true) === false
  modes.value = saveModes(KIND)
  if (disabled.value) return
  warehouses.value = (await warehouseOptions().catch(() => [])) || []
  const saved = uni.getStorageSync(WAREHOUSE_KEY)
  warehouseId.value = warehouses.value.find((w) => w.id === saved)?.id || (warehouses.value.length === 1 ? warehouses.value[0].id : null)
  loadLocations()
})

async function loadLocations() {
  locations.value = warehouseId.value ? (await locationOptions(warehouseId.value).catch(() => [])) || [] : []
  currentLocation.value = null
  rows.value.forEach((r) => (r.locationId = null))
}

function changeWarehouse(e) {
  const id = warehouses.value[e.detail.value]?.id || null
  if (id === warehouseId.value) return
  warehouseId.value = id
  uni.setStorageSync(WAREHOUSE_KEY, id)
  loadLocations()
}

function changeDate(e) {
  bizDate.value = e.detail.value
  fillPrices(rows.value)
}

// ---------------------------------------------------------------- 供应商 / 商品搜索弹层（分页）
const sheet = ref('')
const sheetItems = ref([])
const sheetLoading = ref(false)
const sheetKeyword = ref('')
const sheetHasMore = ref(false)
let sheetPage = 1
let sheetSeq = 0
const materialItem = (m) => ({ id: m.id, title: m.name, sub: [m.code, m.spec, m.unit].filter(Boolean).join(' · '), raw: m })

function openSheet(type) {
  sheetItems.value = []
  sheetHasMore.value = false
  sheet.value = type
}

async function searchSheet(keyword, more = false) {
  const type = sheet.value
  const seq = ++sheetSeq
  const page = more ? sheetPage + 1 : 1
  if (!more) sheetKeyword.value = keyword
  sheetLoading.value = true
  try {
    const list = ((await (type === 'supplier' ? supplierOptions : materialOptions)(keyword, page).catch(() => [])) || [])
    const items = type === 'supplier' ? list.map((s) => ({ id: s.id, title: `${s.code} ${s.name}`, raw: s })) : list.map(materialItem)
    if (seq !== sheetSeq || type !== sheet.value) return
    sheetPage = page
    sheetItems.value = more ? [...sheetItems.value, ...items.filter((it) => !sheetItems.value.some((x) => x.id === it.id))] : items
    sheetHasMore.value = list.length >= OPTION_PAGE_SIZE
  } finally {
    if (seq === sheetSeq) sheetLoading.value = false
  }
}

function selectSheet(item) {
  const type = sheet.value
  sheet.value = ''
  if (type === 'supplier') {
    supplier.value = item?.raw || null
    fillPrices(rows.value)
  } else if (item) {
    addMaterial(item.raw, 1)
  }
}

// ---------------------------------------------------------------- 明细
/** 扫码：库位条码（设为当前库位）→ 物料条码 / 包装条码 → 当前序列号物料的序列号 → 按编码、名称搜索 */
async function onScan(code) {
  const hit = await resolveScan(code, locations.value)
  if (hit.type === 'location') {
    currentLocation.value = hit.location
    if (activeRow.value && !activeRow.value.locationId) activeRow.value.locationId = hit.location.id
    return tip.show(t('wms.common.currentLocationN', { code: hit.location.code }))
  }
  if (hit.type === 'material') return addMaterial(hit.material, hit.ratio, hit.unit)
  const row = activeRow.value
  if (row?.serialEnabled === 1) {
    if (rows.value.some((r) => r.serials.includes(code))) return tip.show(t('wms.common.serialScanned', { code }), false)
    row.serials.push(code)
    row.qty = row.serials.length
    fillPrices([row])
    return tip.show(t('wms.common.serialAdded', { name: row.materialName, code, n: row.serials.length }))
  }
  const list = (await materialOptions(code).catch(() => [])) || []
  const exact = list.find((m) => m.code === code)
  if (exact || list.length === 1) return addMaterial(exact || list[0], 1)
  if (!list.length) return tip.show(t('wms.common.unknownCode', { code }), false)
  openSheet('material')
  sheetItems.value = list.map(materialItem)
}

/** 加入商品：已有该商品（且未分批次）时累加数量；扫包装条码新增的行默认按该包装单位录入 */
function addMaterial(m, ratio = 1, packUnit) {
  let row = rows.value.find((r) => r.materialId === m.id)
  if (row) {
    if (m.serialEnabled !== 1) row.qty = qty4((Number(row.qty) || 0) + ratio / ratioOf(row))
  } else {
    const pack = m.serialEnabled !== 1 && packUnit && packUnit !== m.unit && ratio !== 1
    row = {
      key: rowKey(), materialId: m.id, materialCode: m.code, materialName: m.name, spec: m.spec, unit: m.unit,
      batchEnabled: m.batchEnabled, serialEnabled: m.serialEnabled, qty: m.serialEnabled === 1 ? 0 : ratio, price: null,
      taxRate: newRowTaxRate(), unitName: null, unitRatio: 1, units: [], priceSource: null, priceAuto: true,
      locationId: currentLocation.value?.id || null, batchNo: '', expiryDate: null, serials: [],
      ...(pack ? { qty: 1, unitName: packUnit, unitRatio: ratio } : {})
    }
    rows.value.push(row)
    row = rows.value[rows.value.length - 1]
    attachUnits(row)
  }
  activeRow.value = row
  fillPrices([row])
  if (m.serialEnabled === 1) return tip.show(t('wms.common.scanSerial', { name: m.name }))
  const added = `${qty(ratio)} ${m.unit || ''}${ratio !== 1 && packUnit ? ` (1 ${packUnit})` : ''}`
  tip.show(t('wms.sale.added', { name: row.materialName, qty: added, total: `${qty(row.qty)} ${row.unitName || row.unit || ''}` }))
}

async function attachUnits(row) {
  const units = [...(await loadUnits(row.materialId))]
  if (row.unitName && !units.some((u) => u.unit === row.unitName)) units.push({ unit: row.unitName, ratio: row.unitRatio })
  row.units = units
}

function removeRow(row) {
  rows.value = rows.value.filter((r) => r !== row)
  if (activeRow.value === row) activeRow.value = null
}

function removeSerial(row, s) {
  row.serials = row.serials.filter((x) => x !== s)
  row.qty = row.serials.length
}

/** 按供应商、日期、数量带出默认进价（只覆盖没有手工改过的单价） */
async function fillPrices(list) {
  const targets = list.filter((r) => r.priceAuto)
  if (!targets.length) return
  const res = await defaultPrices({
    kind: 'PURCHASE',
    partnerId: supplier.value?.id,
    materialIds: targets.map((r) => r.materialId),
    qtys: targets.map((r) => baseQty(r) || ratioOf(r)),
    date: bizDate.value
  }).catch(() => [])
  targets.forEach((r, i) => {
    const p = res?.[i]?.materialId === r.materialId ? res[i] : (res || []).find((x) => x.materialId === r.materialId)
    if (!r.priceAuto) return
    r.price = p?.price == null ? null : qty4(Number(p.price) * ratioOf(r))
    if (invoiceKind.value) r.taxRate = newRowTaxRate()
    else if (p?.taxRate != null) r.taxRate = Number(p.taxRate)
    r.priceSource = p?.price == null ? null : p.source
  })
}

// ---------------------------------------------------------------- 保存
function toast(title) {
  uni.showToast({ title, icon: 'none' })
}

/** mode：draft 存草稿 / post 保存并审核（见 tradeSave.js） */
async function submit(mode) {
  if (!supplier.value) return toast(t('trade.purchase.selectSupplier'))
  if (!warehouseId.value) return toast(t('wms.common.selectWarehouseFirst'))
  for (const r of rows.value) {
    if (!(baseQty(r) > 0)) return toast(t('wms.sale.inputQty', { name: r.materialName }))
    if (r.price == null) return toast(t('wms.sale.inputPrice', { name: r.materialName }))
    if (r.batchEnabled === 1 && !r.batchNo) return toast(t('wms.common.inputBatch', { name: r.materialName }))
  }
  saving.value = mode
  try {
    await saveTrade(
      KIND,
      mode,
      async () => {
        // 没有选库位的按上架策略推荐（默认库位 → 同批次 → 同物料 → 空库位）
        await fillPutaway(warehouseId.value, rows.value)
        return {
          partnerId: supplier.value.id,
          warehouseId: warehouseId.value,
          bizDate: bizDate.value,
          remark: remark.value || null,
          invoiceKind: invoiceKind.value || null,
          items: rows.value.map((r) => ({
            materialId: r.materialId,
            qty: baseQty(r),
            price: basePrice(r),
            taxRate: r.taxRate,
            unitName: r.unitName || null,
            locationId: r.locationId || null,
            batchNo: r.batchEnabled === 1 ? r.batchNo : null,
            expiryDate: r.batchEnabled === 1 ? r.expiryDate || null : null,
            serialNos: r.serialEnabled === 1 ? r.serials.join(',') : null
          }))
        }
      },
      { then: reset }
    )
  } finally {
    saving.value = ''
  }
}

/** 保存后清空明细，保留供应商、仓库，便于连续开单 */
function reset() {
  rows.value = []
  activeRow.value = null
  remark.value = ''
  tip.text = ''
}
</script>

<style lang="scss">
@import '../mobile.scss';

.pur {
  &__scan {
    display: flex;
    align-items: center;
  }
  &__scan-input {
    flex: 1;
    min-width: 0;
  }
  &__search {
    padding-right: 24rpx;
  }
  &__sum {
    padding: 8rpx 0;
    font-size: 28rpx;
  }
  &__save {
    padding: 0 16rpx;
    font-size: 28rpx;
  }
}
</style>
