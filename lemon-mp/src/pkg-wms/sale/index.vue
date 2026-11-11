<template>
  <view>
    <l-empty v-if="disabled" :text="t('wms.sale.disabled')" />
    <block v-else>
      <!-- 表头：客户、仓库、业务员、日期 -->
      <view class="m-card">
        <view class="m-input" @click="openSheet('customer')">
          <text class="m-input__label">{{ t('wms.sale.customer') }}</text>
          <text class="m-input__value" :class="{ 'l-placeholder': !customer }">{{ customer ? `${customer.code} ${customer.name}` : t('common.pleaseSelect') }}</text>
        </view>
        <picker :range="warehouses" range-key="name" :value="warehouseIndex" @change="changeWarehouse">
          <view class="m-input">
            <text class="m-input__label">{{ t('wms.common.warehouse') }}</text>
            <text class="m-input__value" :class="{ 'l-placeholder': !warehouseId }">{{ warehouseName || t('common.pleaseSelect') }}</text>
          </view>
        </picker>
        <view class="m-input" @click="openSheet('salesman')">
          <text class="m-input__label">{{ t('wms.sale.salesman') }}</text>
          <text class="m-input__value" :class="{ 'l-placeholder': !salesman }">{{ salesman ? salesman.title : t('wms.sale.salesmanDefault') }}</text>
        </view>
        <picker mode="date" :value="bizDate" @change="changeDate">
          <view class="m-input">
            <text class="m-input__label">{{ t('wms.sale.bizDate') }}</text>
            <text class="m-input__value">{{ bizDate }}</text>
          </view>
        </picker>
      </view>

      <!-- 商品：扫码或搜索 -->
      <view class="sale__scan">
        <view class="sale__scan-input"><l-scan-input :placeholder="t('wms.sale.scanTip')" @scan="onScan" /></view>
        <text class="m-link sale__search" @click="openSheet('material')">{{ t('wms.sale.searchMaterial') }}</text>
      </view>
      <view v-if="tip.text" class="m-tip" :class="{ 'm-tip--fail': !tip.ok }">{{ tip.text }}</view>

      <SaleRow
        v-for="row in rows"
        :key="row.key"
        :row="row"
        :active="row === activeRow"
        :line-warehouse="lineWarehouse"
        :warehouses="warehouses"
        :warehouse-id="rowWarehouse(row)"
        :promo-line="promoLine(row)"
        @activate="activeRow = row"
        @remove="removeRow(row)"
        @remove-serial="(s) => removeSerial(row, s)"
        @reference="openReference(row)"
        @changed="fillPrices([row])"
        @warehouse="reloadSlots(row)"
      />
      <!-- 买赠赠品：按促销预览自动带出，可选仓库、库位批次；明细可选仓库时可拆成多行从不同仓库出（数量合计等于赠品数量） -->
      <SaleRow
        v-for="row in giftRows"
        :key="row.key"
        :row="row"
        :active="row === activeRow"
        :line-warehouse="lineWarehouse"
        :warehouses="warehouses"
        :warehouse-id="rowWarehouse(row)"
        :gift-split="giftGroup(row).length > 1"
        :gift-first="giftGroup(row)[0] === row"
        @activate="activeRow = row"
        @remove="removeGiftSplit(row)"
        @remove-serial="(s) => removeSerial(row, s)"
        @split="splitGift(row)"
        @gift-qty="(v) => changeGiftQty(row, v)"
        @warehouse="reloadSlots(row)"
      />
      <l-empty v-if="!rows.length" :text="t('wms.sale.emptyRows')" />

      <!-- 合计、整单优惠 / 抹零 -->
      <view v-if="rows.length" class="m-card">
        <view class="m-row sale__sum">
          <text>{{ t('wms.sale.total') }}</text>
          <text>{{ money(totalAmount) }}</text>
        </view>
        <view v-if="promoDiscount > 0" class="m-row sale__sum sale__promo">
          <text>{{ t('wms.sale.promoTotal') }}</text>
          <text>-{{ money(promoDiscount) }}</text>
        </view>
        <view class="m-input">
          <text class="m-input__label">{{ t('wms.sale.wholeDiscount') }}</text>
          <input :value="wholeDiscount" type="digit" :placeholder="t('wms.sale.wholeDiscountPh')" placeholder-class="l-placeholder" @blur="(e) => changeDiscount(e.detail.value)" />
        </view>
        <view class="sale__rounds">
          <text class="sale__round" @click="roundDown(0.1)">{{ t('wms.sale.roundFen') }}</text>
          <text class="sale__round" @click="roundDown(1)">{{ t('wms.sale.roundJiao') }}</text>
          <text class="sale__round" @click="roundDown(10)">{{ t('wms.sale.roundYuan') }}</text>
          <text v-if="wholeDiscount" class="sale__round" @click="wholeDiscount = null">{{ t('wms.common.clear') }}</text>
        </view>
        <view class="m-input">
          <text class="m-input__label">{{ t('common.remark') }}</text>
          <input v-model="remark" maxlength="255" :placeholder="t('wms.sale.optional')" placeholder-class="l-placeholder" />
        </view>
      </view>

      <SaleFreight v-if="rows.length" ref="freightRef" />

      <view class="l-footer-space" />
      <view class="l-footer">
        <view class="m-footer-info">
          {{ t('wms.sale.payable') }}<text class="m-strong">{{ money(payable) }}</text>
        </view>
        <button class="l-btn" :loading="saving" :disabled="saving || !rows.length" @click="submit">{{ t('wms.sale.submit') }}</button>
      </view>
    </block>

    <SearchSheet
      :open="!!sheet"
      :title="sheetTitle"
      :items="sheetItems"
      :loading="sheetLoading"
      :empty-option="sheet === 'salesman' ? t('wms.sale.salesmanDefault') : ''"
      @search="searchSheet"
      @select="selectSheet"
      @close="sheet = ''"
    />
    <PriceRefSheet ref="refSheet" @pick="pickPrice" />
  </view>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { addSalesOut, addSalesOutDraft, customerOptions, defaultPrices, materialOptions, previewPromotion, warehouseOptions } from '@/api/wms'
import { pageIdentityUsers } from '@/api/workflow'
import { useTitle } from '@/composables/useTitle'
import { runAction } from '@/composables/useAction'
import { bizRule, loadBizRules } from '@/store/bizRules'
import { hasPerm } from '@/store/user'
import { ensureLogin } from '@/utils/session'
import { money, qty, today } from '@/utils/format'
import { qty4, resolveScan, rowKey, useScanTip } from '../mobile'
import { baseQty, basePrice, lineAmount, loadSlots, loadUnits, overStock, ratioOf } from './lines'
import { useGiftRows } from './gifts'
import SaleRow from './SaleRow.vue'
import SaleFreight from './SaleFreight.vue'
import SearchSheet from '../components/SearchSheet.vue'
import PriceRefSheet from '../components/PriceRefSheet.vue'

/**
 * 快速开单：销售出库直接开单（不引用订单，与 Web 端销售出库「直接开单」相同的接口 POST /scm/salesOut）。
 * 选客户、仓库、业务员（默认客户负责人，没有时为本人），扫码或搜索商品，单价按 /scm/pricing/default 带出（可查看价格参考），
 * 每行按先到期先出自动选择库位 / 批次，序列号物料扫描序列号；可录入整单优惠或抹零。
 * 物料有辅助单位时每行可选录入单位（扫包装条码时默认该包装单位），提交时折算为基本单位；
 * 业务参数 scm.salesOut.lineWarehouse 开启时每行可另选仓库（为空取表头仓库）；
 * 买赠促销的赠品按促销预览自动带出（单价 0，数量不能改，可选仓库、库位批次）。业务参数 scm.directDoc.enabled 关闭时不可用
 */
const { t } = useI18n()
useTitle('wms.entry.quickSale')
const tip = useScanTip()
const WAREHOUSE_KEY = 'lemon_mp_sale_warehouse'

const disabled = ref(false)
const customer = ref(null)
const salesman = ref(null)
const warehouses = ref([])
const warehouseId = ref(null)
const bizDate = ref(today())
const remark = ref('')
const rows = ref([])
const activeRow = ref(null)
const wholeDiscount = ref(null)
const saving = ref(false)
/** 运费（SaleFreight） */
const freightRef = ref()

const warehouseIndex = computed(() => Math.max(0, warehouses.value.findIndex((w) => w.id === warehouseId.value)))
const warehouseName = computed(() => warehouses.value.find((w) => w.id === warehouseId.value)?.name)
/** 业务参数：销售出库明细可选仓库（默认开启，与后端一致） */
const lineWarehouse = computed(() => bizRule('scm.salesOut.lineWarehouse', true) === true)
/** 业务参数：默认税率（%），新明细的税率；读取失败时为空，由后端按默认税率处理（issue #82） */
const defaultTaxRate = computed(() => {
  const v = bizRule('scm.defaultTaxRate', null)
  return v == null || v === '' ? null : Number(v)
})

onLoad(async () => {
  if (!(await ensureLogin())) return
  await loadBizRules(['scm.directDoc.enabled', 'scm.salesOut.lineWarehouse', 'scm.tradeDoc.draft', 'scm.defaultTaxRate'])
  disabled.value = bizRule('scm.directDoc.enabled', true) === false
  if (disabled.value) return
  warehouses.value = (await warehouseOptions().catch(() => [])) || []
  const saved = uni.getStorageSync(WAREHOUSE_KEY)
  warehouseId.value = warehouses.value.find((w) => w.id === saved)?.id || (warehouses.value.length === 1 ? warehouses.value[0].id : null)
})

/** 买赠的赠品行（由促销预览生成，可拆分）；newRow、reloadSlots 是函数声明，可以先引用 */
const { giftRows, applyGifts, giftGroup, splitGift, changeGiftQty, removeGiftSplit, mismatchRow } = useGiftRows({ newRow, reloadSlots, activeRow })

// ---------------------------------------------------------------- 表头
/** 本行实际出库的仓库：明细仓库，为空（或参数关闭）取表头仓库 */
const rowWarehouse = (row) => (lineWarehouse.value && row.warehouseId) || warehouseId.value

/** 重新加载库位批次；仓库变了时清空已扫的序列号 */
function reloadSlots(row) {
  loadSlots(row, rowWarehouse(row))
}

function changeWarehouse(e) {
  const id = warehouses.value[e.detail.value]?.id || null
  if (id === warehouseId.value) return
  warehouseId.value = id
  uni.setStorageSync(WAREHOUSE_KEY, id)
  // 沿用表头仓库的行：库位批次、序列号跟着换
  allRows().filter((r) => !(lineWarehouse.value && r.warehouseId)).forEach((r) => {
    clearSerials(r)
    reloadSlots(r)
  })
}

function changeDate(e) {
  bizDate.value = e.detail.value
  fillPrices(rows.value)
}

const allRows = () => [...rows.value, ...giftRows.value]

// 客户 / 业务员 / 商品的搜索弹层
const sheet = ref('')
const sheetItems = ref([])
const sheetLoading = ref(false)
const sheetTitle = computed(() => (sheet.value ? t(`wms.sale.sheet.${sheet.value}`) : ''))
const materialItem = (m) => ({ id: m.id, title: m.name, sub: [m.code, m.spec, m.unit].filter(Boolean).join(' · '), raw: m })

function openSheet(type) {
  sheetItems.value = []
  sheet.value = type
}

async function searchSheet(keyword) {
  const type = sheet.value
  sheetLoading.value = true
  try {
    if (type === 'customer') {
      const list = (await customerOptions(keyword).catch(() => [])) || []
      sheetItems.value = list.map((c) => ({ id: c.id, title: `${c.code} ${c.name}`, raw: c }))
    } else if (type === 'salesman') {
      const res = await pageIdentityUsers({ current: 1, size: 30, username: keyword || undefined }).catch(() => null)
      sheetItems.value = (res?.records || []).map((u) => ({ id: u.id, title: u.nickname || u.username, sub: [u.username, u.deptName].filter(Boolean).join(' · ') }))
    } else if (type === 'material') {
      const list = (await materialOptions(keyword).catch(() => [])) || []
      sheetItems.value = list.map(materialItem)
    }
  } finally {
    sheetLoading.value = false
  }
}

function selectSheet(item) {
  const type = sheet.value
  sheet.value = ''
  if (type === 'customer') {
    customer.value = item?.raw || null
    fillPrices(rows.value)
  } else if (type === 'salesman') {
    salesman.value = item
  } else if (type === 'material' && item) {
    addMaterial(item.raw, 1)
  }
}

// ---------------------------------------------------------------- 明细
/** 扫码：物料条码 / 包装条码（按换算比例累加）；序列号物料选中后扫描序列号；都不是时按编码、名称搜索 */
async function onScan(code) {
  const hit = await resolveScan(code)
  if (hit.type === 'material') return addMaterial(hit.material, hit.ratio, hit.unit)
  const row = activeRow.value
  if (row?.serialEnabled === 1) {
    if (allRows().some((r) => r.serials.includes(code))) return tip.show(t('wms.common.serialScanned', { code }), false)
    // 赠品的数量由促销决定，序列号不能超过赠品数量
    if (row.gift && row.serials.length >= Number(row.qty)) return tip.show(t('wms.sale.giftSerialFull', { name: row.materialName }), false)
    row.serials.push(code)
    if (!row.gift) {
      row.qty = row.serials.length
      fillPrices([row])
    }
    return tip.show(t('wms.common.serialAdded', { name: row.materialName, code, n: row.serials.length }))
  }
  const list = (await materialOptions(code).catch(() => [])) || []
  const exact = list.find((m) => m.code === code)
  if (exact || list.length === 1) return addMaterial(exact || list[0], 1)
  if (!list.length) return tip.show(t('wms.common.unknownCode', { code }), false)
  // 多个候选：打开搜索弹层选择
  openSheet('material')
  sheetItems.value = list.map(materialItem)
}

/** 新的明细行（qty、price 为所选单位的数量、单价） */
function newRow(m, extra) {
  return {
    key: rowKey(), materialId: m.id, materialCode: m.code, materialName: m.name, spec: m.spec, unit: m.unit,
    batchEnabled: m.batchEnabled, serialEnabled: m.serialEnabled, qty: 0, price: null, taxRate: defaultTaxRate.value,
    unitName: null, unitRatio: 1, units: [], warehouseId: null,
    priceSource: null, priceAuto: true, slots: [], slotIndex: 0, slotsLoading: false, serials: [], gift: false,
    ...extra
  }
}

/**
 * 加入商品：已有该商品时累加数量（换算到该行的录入单位）；
 * 扫包装条码新增的行默认按该包装单位录入（序列号物料只按基本单位）
 *
 * @param ratio    本次增加的基本单位数量（包装条码为换算率）
 * @param packUnit 包装单位
 */
function addMaterial(m, ratio = 1, packUnit) {
  let row = rows.value.find((r) => r.materialId === m.id)
  if (row) {
    // 序列号物料的数量 = 扫描的序列号个数
    if (m.serialEnabled !== 1) row.qty = qty4((Number(row.qty) || 0) + ratio / ratioOf(row))
  } else {
    const pack = m.serialEnabled !== 1 && packUnit && packUnit !== m.unit && ratio !== 1
    rows.value.push(newRow(m, pack ? { qty: 1, unitName: packUnit, unitRatio: ratio } : { qty: m.serialEnabled === 1 ? 0 : ratio }))
    row = rows.value[rows.value.length - 1]
    reloadSlots(row)
    attachUnits(row)
  }
  activeRow.value = row
  fillPrices([row])
  if (m.serialEnabled === 1) return tip.show(t('wms.common.scanSerial', { name: m.name }))
  const added = `${qty(ratio)} ${m.unit || ''}${ratio !== 1 && packUnit ? ` (1 ${packUnit})` : ''}`
  tip.show(t('wms.sale.added', { name: row.materialName, qty: added, total: `${qty(row.qty)} ${row.unitName || row.unit || ''}` }))
}

/** 读取物料的辅助单位（扫码使用的包装单位不在设置中时也保留） */
async function attachUnits(row) {
  const units = [...(await loadUnits(row.materialId))]
  if (row.unitName && !units.some((u) => u.unit === row.unitName)) units.push({ unit: row.unitName, ratio: row.unitRatio })
  row.units = units
}

function removeRow(row) {
  rows.value = rows.value.filter((r) => r !== row)
  if (activeRow.value === row) activeRow.value = null
}

function clearSerials(row) {
  row.serials = []
  if (row.serialEnabled === 1 && !row.gift) row.qty = 0
}

function removeSerial(row, s) {
  row.serials = row.serials.filter((x) => x !== s)
  if (!row.gift) row.qty = row.serials.length
}

// ---------------------------------------------------------------- 单价：默认价、价格参考（含税、基本单位，按行的录入单位折算）
/** 按客户、日期、数量带出默认价（只覆盖没有手工改过的单价）；按行返回，同一物料多行时各自按数量匹配起订量 */
async function fillPrices(list) {
  const targets = list.filter((r) => r.priceAuto && !r.gift)
  if (!targets.length || !customer.value) return
  const res = await defaultPrices({
    kind: 'SALES',
    partnerId: customer.value.id,
    materialIds: targets.map((r) => r.materialId),
    qtys: targets.map((r) => baseQty(r) || ratioOf(r)),
    date: bizDate.value
  }).catch(() => [])
  targets.forEach((r, i) => {
    const p = res?.[i]?.materialId === r.materialId ? res[i] : (res || []).find((x) => x.materialId === r.materialId)
    if (!p || !r.priceAuto || p.price == null) return
    r.price = qty4(Number(p.price) * ratioOf(r))
    if (p.taxRate != null) r.taxRate = Number(p.taxRate)
    r.priceSource = p.source
  })
}

const refSheet = ref()
let refRow = null
function openReference(row) {
  refRow = row
  refSheet.value.open({ materialId: row.materialId, materialName: row.materialName, partnerId: customer.value?.id })
}
function pickPrice(p) {
  if (!refRow) return
  Object.assign(refRow, { price: qty4(p * ratioOf(refRow)), priceAuto: false, priceSource: null })
}

// ---------------------------------------------------------------- 促销预览（折扣、满减、买赠）、整单优惠
const promo = ref(null)
/** 明细变化后促销预览还没有返回（赠品可能未更新），此时不能保存 */
const promoPending = ref(false)
let promoTimer
let promoSeq = 0
watch(
  () => JSON.stringify([customer.value?.id, bizDate.value, rows.value.map((r) => [r.materialId, baseQty(r), basePrice(r)])]),
  () => {
    clearTimeout(promoTimer)
    const ready = customer.value && rows.value.length && rows.value.every((r) => baseQty(r) > 0 && r.price != null)
    if (!ready || !canPreview()) {
      promoSeq++
      promoPending.value = false
      return setPromo(null)
    }
    promoPending.value = true
    promoTimer = setTimeout(async () => {
      const seq = ++promoSeq
      const res = await previewPromotion({
        customerId: customer.value.id,
        orderDate: bizDate.value,
        items: rows.value.map((r) => ({ materialId: r.materialId, qty: baseQty(r), price: basePrice(r) }))
      }).catch(() => null)
      if (seq !== promoSeq) return
      setPromo(res)
      promoPending.value = false
    }, 500)
  }
)
const promoLine = (row) => {
  const line = promo.value?.lines?.[rows.value.indexOf(row)]
  return line && line.origPrice != null ? line : null
}

/** 促销预览结果；赠品行按预览重建 */
function setPromo(res) {
  promo.value = res
  applyGifts(res?.gifts)
}

/** 促销预览的权限（与后端 /scm/promotion/preview 一致）；没有时不显示赠品，由后端保存时计算并自动补上赠品 */
const canPreview = () => hasPerm(['scm:sales:add', 'scm:sales:edit', 'scm:salesOut:add'])

const totalAmount = computed(() => Math.round(rows.value.reduce((s, r) => s + lineAmount(r), 0) * 100) / 100)
const promoDiscount = computed(() => Number(promo.value?.discountAmount) || 0)
/** 整单优惠的基数：扣除促销优惠后的合计（与 Web 端一致） */
const discountBase = computed(() => Math.round((totalAmount.value - promoDiscount.value) * 100) / 100)
const payable = computed(() => Math.round((discountBase.value - (Number(wholeDiscount.value) || 0)) * 100) / 100)

function changeDiscount(v) {
  const n = v === '' ? 0 : Number(v)
  if (!Number.isFinite(n) || n <= 0) return (wholeDiscount.value = null)
  wholeDiscount.value = Math.min(Math.round(n * 100) / 100, Math.max(discountBase.value, 0))
}

/** 抹零：优惠后合计向下取整到 unit（0.1 / 1 / 10），差额累加到优惠（按分计算避免浮点误差） */
function roundDown(unit) {
  const cents = Math.round(payable.value * 100)
  if (cents <= 0) return
  const off = cents % Math.round(unit * 100)
  if (off) wholeDiscount.value = Math.round((Number(wholeDiscount.value) || 0) * 100 + off) / 100
}

// ---------------------------------------------------------------- 保存
function toast(title) {
  uni.showToast({ title, icon: 'none' })
}

/** 提交的明细：数量、单价折算为基本单位（unitName 为录入单位，仅显示）；明细仓库只在与表头不同时传 */
function toItem(r, slot = r.slots[r.slotIndex], qtyOverride) {
  const own = lineWarehouse.value && r.warehouseId && r.warehouseId !== warehouseId.value ? r.warehouseId : null
  return {
    materialId: r.materialId,
    qty: qtyOverride ?? baseQty(r),
    price: r.gift ? 0 : basePrice(r),
    taxRate: r.gift ? null : r.taxRate,
    unitName: r.gift ? null : r.unitName || null,
    gift: r.gift || null,
    locationId: slot?.locationId || null,
    batchNo: slot?.batchNo || null,
    warehouseId: own,
    serialNos: r.serialEnabled === 1 ? r.serials.join(',') : null
  }
}

/**
 * 赠品行：所选库位批次不够时按先到期先出从其他库位批次补足，拆成多行（后端按赠品物料合计校验数量）；
 * 序列号赠品不拆（序列号所在的库位批次由所选项决定）
 */
function giftItems(r) {
  if (r.serialEnabled === 1 || !overStock(r)) return [toItem(r)]
  const order = [r.slots[r.slotIndex], ...r.slots.filter((_, i) => i !== r.slotIndex)]
  const items = []
  let left = baseQty(r)
  for (const slot of order) {
    if (left <= 0) break
    const take = qty4(Math.min(left, slot.available))
    if (take > 0) items.push(toItem(r, slot, take))
    left = qty4(left - take)
  }
  return left > 0 ? null : items
}

async function submit() {
  if (!customer.value) return toast(t('wms.sale.selectCustomer'))
  if (!warehouseId.value) return toast(t('wms.common.selectWarehouseFirst'))
  if (promoPending.value) return toast(t('wms.sale.promoLoading'))
  for (const r of rows.value) {
    if (!(baseQty(r) > 0)) return toast(t('wms.sale.inputQty', { name: r.materialName }))
    if (r.price == null) return toast(t('wms.sale.inputPrice', { name: r.materialName }))
  }
  for (const r of allRows()) {
    if (r.slotsLoading) return toast(t('common.loading'))
    if (!r.slots.length) return toast(t('wms.sale.noStockOf', { name: r.materialName }))
    if (r.gift && !(Number(r.qty) > 0)) continue
    if (r.gift && !giftItems(r)) return toast(t('wms.sale.giftStockShort', { name: r.materialName, n: qty(r.qty) }))
    if (r.gift && r.serialEnabled === 1 && r.serials.length !== Number(r.qty)) {
      return toast(t('wms.sale.giftSerialCount', { name: r.materialName, n: qty(r.qty) }))
    }
  }
  const mismatch = mismatchRow()
  if (mismatch) return toast(t('wms.sale.giftQtyMismatch', { name: mismatch.materialName, n: qty(mismatch.giftTotal) }))
  if (payable.value < 0) return toast(t('wms.sale.discountTooLarge'))
  const freightError = freightRef.value?.check()
  if (freightError) return toast(freightError)
  // 业务参数「进销存单据先存草稿」开启且没有审核权限时保存为草稿
  const asDraft = bizRule('scm.tradeDoc.draft', false) === true && !hasPerm('scm:salesOut:post')
  saving.value = true
  try {
    await runAction(
      () =>
        (asDraft ? addSalesOutDraft : addSalesOut)({
          partnerId: customer.value.id,
          salesmanId: salesman.value?.id || null,
          warehouseId: warehouseId.value,
          bizDate: bizDate.value,
          remark: remark.value || null,
          wholeDiscount: wholeDiscount.value || null,
          ...(freightRef.value?.payload() || {}),
          items: [...rows.value.map((r) => toItem(r)), ...giftRows.value.filter((r) => Number(r.qty) > 0).flatMap(giftItems)]
        }),
      { success: asDraft ? t('wms.sale.draftSaved') : t('wms.sale.done'), then: reset }
    )
  } finally {
    saving.value = false
  }
}

/** 保存后清空明细，保留客户、仓库、业务员，便于连续开单 */
function reset() {
  rows.value = []
  giftRows.value = []
  activeRow.value = null
  wholeDiscount.value = null
  freightRef.value?.reset()
  remark.value = ''
  promo.value = null
  tip.text = ''
}
</script>

<style lang="scss">
@import '../mobile.scss';

.sale {
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
  &__promo {
    margin-top: 8rpx;
    font-size: 24rpx;
    color: $l-success;
  }
  &__sum {
    padding: 8rpx 0;
    font-size: 28rpx;
  }
  &__rounds {
    display: flex;
    flex-wrap: wrap;
    gap: 16rpx;
    margin-top: 16rpx;
  }
  &__round {
    padding: 8rpx 24rpx;
    border-radius: 28rpx;
    border: 1rpx solid $l-primary;
    color: $l-primary;
    font-size: 24rpx;
  }
}
</style>
