<template>
  <view>
    <!-- 选择盘点单 -->
    <block v-if="!check">
      <view class="m-tip check__hint">{{ t('wms.check.tip') }}</view>
      <!-- 新建盘点：选择仓库后直接扫码 / 搜索物料录入，保存时生成盘点单（草稿） -->
      <view v-if="canAdd" class="m-card">
        <picker :range="warehouses" range-key="name" @change="(e) => (newWarehouse = warehouses[e.detail.value] || null)">
          <view class="m-input">
            <text class="m-input__label">{{ t('wms.common.warehouse') }}</text>
            <text class="m-input__value" :class="{ 'l-placeholder': !newWarehouse }">{{ newWarehouse?.name || t('common.pleaseSelect') }}</text>
          </view>
        </picker>
        <button class="l-btn check__new-btn" :disabled="!newWarehouse" @click="startNew">{{ t('wms.check.newCheck') }}</button>
      </view>
      <view v-for="c in checks" :key="c.id" class="m-card" @click="selectCheck(c.id)">
        <view class="m-row">
          <text class="m-title">{{ c.docNo }}</text>
          <text class="m-sub m-nowrap">{{ c.bizDate }}</text>
        </view>
        <view class="m-sub">{{ c.warehouseName }}{{ c.remark ? ` · ${c.remark}` : '' }}</view>
      </view>
      <l-empty v-if="loaded && !checks.length" :text="t('wms.check.noChecks')" />
    </block>

    <!-- 按库位扫码盘点 -->
    <block v-else>
      <view class="m-card">
        <view class="m-row">
          <text class="m-title">{{ check.docNo || t('wms.check.newCheck') }}</text>
          <text class="m-link" @click="leave">{{ t('wms.putaway.change') }}</text>
        </view>
        <view class="m-sub">{{ check.warehouseName }} · {{ t('wms.check.counted', { n: countedCount, total: items.length }) }}</view>
      </view>
      <l-scan-input :placeholder="t('wms.common.scanLocThenItem')" @scan="onScan" />
      <view class="check__search m-link" @click="sheetOpen = true">{{ t('wms.check.searchAdd') }}</view>
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
          <l-tag v-if="row.added" :text="row.bookQty ? t('wms.check.newLine') : t('wms.check.gain')" :color="row.bookQty ? 'blue' : 'green'" />
        </view>
        <view class="m-sub">{{ row.spec || '' }}{{ row.batchNo && !gainRow(row) ? t('wms.common.batchN', { no: row.batchNo }) : '' }}</view>
        <view v-if="gainRow(row) && row.batchEnabled === 1" class="m-input">
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
        <!-- 盘盈行的成本价：后端返回 costVisible（有成本权限或可编辑盘点单）时显示，为空按确认时的当前均价 -->
        <view v-if="check.costVisible && isGain(row)" class="m-input">
          <text class="m-input__label">{{ t('wms.check.costPrice') }}</text>
          <input
            :value="row.costPrice"
            type="digit"
            :placeholder="row.avgCost != null ? t('wms.check.costPricePh', { n: price(row.avgCost) }) : t('wms.check.costPriceDefault')"
            placeholder-class="l-placeholder"
            @blur="(e) => changeCost(row, e.detail.value)"
          />
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
    <SearchSheet
      :open="sheetOpen"
      :title="t('wms.check.searchAdd')"
      :items="sheetItems"
      :loading="sheetLoading"
      :has-more="sheetHasMore"
      @search="searchMaterials"
      @more="searchMaterials(sheetKeyword, true)"
      @select="onPick"
      @close="sheetOpen = false"
    />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { WmsDocStatus } from '@web/constants/enums'
import { addCheck, checkMaterialItems, getCheck, locationOptions, materialOptions, OPTION_PAGE_SIZE, pageCheck, updateCheck, warehouseOptions } from '@/api/wms'
import { hasPerm } from '@/store/user'
import { useTitle } from '@/composables/useTitle'
import { runAction } from '@/composables/useAction'
import { price, qty, today } from '@/utils/format'
import SearchSheet from '../components/SearchSheet.vue'
import { locationText, qty4, resolveScan, rowKey, splitSerials, toNumber, useScanTip } from '../mobile'

/**
 * 盘点：选择草稿盘点单，扫描库位后扫描物料累计实盘数量（包装条码按换算比例），序列号物料扫描序列号；
 * 没有条码的可搜索物料后录入。不在明细中的物料随扫随加（issue #123）：盘点仓库中有库存的按库存位置带出账面数量，没有库存的作为盘盈行添加；
 * 盘盈行可填成本价（默认确认时的当前均价）。有新增权限时可选择仓库直接开始盘点，第一次保存时生成盘点单（草稿）。
 * 保存实盘数量，盘点确认仍在后台进行
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

const canAdd = hasPerm('wms:check:add')
const warehouses = ref([])
const newWarehouse = ref(null)

onLoad(() => {
  if (canAdd) warehouseOptions().then((list) => (warehouses.value = list || [])).catch(() => {})
  loadChecks()
})

async function loadChecks() {
  try {
    checks.value = (await pageCheck({ status: WmsDocStatus.DRAFT, current: 1, size: 50 })).records || []
  } finally {
    loaded.value = true
  }
}

/** 新建盘点：明细为空，扫码 / 搜索物料逐个加入，保存时新增盘点单 */
async function startNew() {
  const w = newWarehouse.value
  items.value = []
  locations.value = await locationOptions(w.id)
  check.value = { warehouseId: w.id, warehouseName: w.name, bizDate: today(), remark: '', freeze: false, items: [] }
  currentLocation.value = null
  activeRow.value = null
  tip.text = ''
}

async function selectCheck(id) {
  const c = await getCheck(id)
  items.value = (c.items || []).map((i) => ({
    ...i, key: `item-${i.id}`, bookQty: Number(i.bookQty), actualQty: i.actualQty == null ? null : Number(i.actualQty),
    serials: splitSerials(i.serialNos), added: false, costPrice: i.costPrice == null ? null : Number(i.costPrice)
  }))
  locations.value = await locationOptions(c.warehouseId)
  check.value = c
  currentLocation.value = null
  activeRow.value = null
  tip.text = ''
}

function leave() {
  check.value = null
  loadChecks()
}

const shown = computed(() =>
  currentLocation.value && onlyCurrent.value ? items.value.filter((r) => r.locationId === currentLocation.value.id) : items.value
)
const actualOf = (r) => (r.serialEnabled === 1 ? (r.serials.length || r.actualQty != null ? r.serials.length : null) : r.actualQty)
const countedCount = computed(() => items.value.filter((r) => actualOf(r) != null).length)
const diffCount = computed(() => items.value.filter((r) => actualOf(r) != null && qty4(actualOf(r)) !== r.bookQty).length)
/** 盘盈：实盘大于账面 */
const isGain = (r) => actualOf(r) != null && qty4(actualOf(r)) > r.bookQty
const diffClass = (r) => (r.actualQty == null ? '' : qty4(r.actualQty) === r.bookQty ? 'qty-done' : 'qty-short')

async function onScan(code) {
  const hit = await resolveScan(code, locations.value)
  if (hit.type === 'location') {
    currentLocation.value = hit.location
    activeRow.value = null
    return tip.show(t('wms.check.locationLines', { code: hit.location.code, n: shown.value.length }))
  }
  if (hit.type === 'material') {
    const row = await locate(hit.material)
    if (row.serialEnabled === 1) return tip.show(t('wms.common.scanSerial', { name: row.materialName }))
    row.actualQty = qty4((row.actualQty || 0) + hit.ratio)
    return tip.show(t(gainRow(row) ? 'wms.check.addedGain' : 'wms.check.added', { name: row.materialName, qty: qty(hit.ratio), actual: qty(row.actualQty) }))
  }
  const row = activeRow.value
  if (row?.serialEnabled !== 1) return tip.show(t('wms.common.unknownCode', { code }), false)
  if (items.value.some((r) => r.serials.includes(code))) return tip.show(t('wms.common.serialScanned', { code }), false)
  row.serials.push(code)
  tip.show(t('wms.check.serialAdded', { name: row.materialName, code, n: row.serials.length }))
}

/** 手机上添加、账面为 0 的行（盘盈），需要填写批次号 */
const gainRow = (r) => r.added && !r.bookQty
const slotKey = (r) => `${r.locationId || 0}|${r.materialId}|${r.batchNo || ''}`

/**
 * 物料对应的明细行：扫描了库位时找该库位的行，否则找该物料的任一行；明细中没有时加入——
 * 盘点仓库中有库存的按库存位置各加一行（带出账面数量，扫描了库位时只加该库位的），没有库存的加一行账面 0（盘盈）
 */
async function locate(m) {
  const locationId = currentLocation.value?.id || null
  const matches = (r) => r.materialId === m.id && (!locationId || (r.locationId || null) === locationId)
  let list = items.value.filter(matches)
  if (!list.length) {
    const exists = new Set(items.value.map(slotKey))
    // 取不到库存时按账面 0 添加，保存时后端仍按当时的库存计算账面数量
    const slots = (await checkMaterialItems({ warehouseId: check.value.warehouseId, materialIds: [m.id] }).catch(() => [])) || []
    const added = slots
      .filter((s) => Number(s.bookQty) !== 0 && (!locationId || (s.locationId || null) === locationId) && !exists.has(slotKey(s)))
      .map((s) => ({
        ...s, key: rowKey(), locationId: s.locationId || null, bookQty: Number(s.bookQty), actualQty: null, serialNos: null, serials: [],
        costPrice: null, added: true
      }))
    if (!added.length) {
      added.push({
        key: rowKey(), materialId: m.id, materialCode: m.code, materialName: m.name, spec: m.spec, unit: m.unit, batchEnabled: m.batchEnabled,
        serialEnabled: m.serialEnabled, locationId, locationCode: currentLocation.value?.code, batchNo: '', bookQty: 0, actualQty: null, serials: [], added: true
      })
    }
    items.value.push(...added)
    list = items.value.filter(matches)
  }
  const row = list.find((r) => r === activeRow.value) || list[0]
  activeRow.value = row
  return row
}

// ---------------------------------------------------------------- 搜索物料（没有条码时）
const sheetOpen = ref(false)
const sheetItems = ref([])
const sheetLoading = ref(false)
const sheetHasMore = ref(false)
const sheetKeyword = ref('')
let sheetPage = 1
let sheetSeq = 0

async function searchMaterials(keyword, more = false) {
  const seq = ++sheetSeq
  const page = more ? sheetPage + 1 : 1
  if (!more) sheetKeyword.value = keyword
  sheetLoading.value = true
  try {
    const rows = (await materialOptions(keyword || undefined, page).catch(() => [])) || []
    if (seq !== sheetSeq) return
    sheetPage = page
    const mapped = rows.map((m) => ({ ...m, title: `${m.code} ${m.name}`, sub: m.spec || '' }))
    sheetItems.value = more ? [...sheetItems.value, ...mapped] : mapped
    sheetHasMore.value = rows.length >= OPTION_PAGE_SIZE
  } finally {
    if (seq === sheetSeq) sheetLoading.value = false
  }
}

/** 搜索选择物料：定位到（或加入）该物料的明细行，由盘点人录入实盘数量 */
async function onPick(m) {
  sheetOpen.value = false
  if (!m) return
  const row = await locate(m)
  tip.show(t(row.serialEnabled === 1 ? 'wms.common.scanSerial' : 'wms.check.inputActual', { name: row.materialName }))
}

function changeCost(row, v) {
  const n = v === '' ? null : Number(v)
  row.costPrice = n != null && Number.isFinite(n) && n >= 0 ? Math.round(n * 1000000) / 1000000 : null
}

function removeSerial(row, s) {
  row.serials = row.serials.filter((x) => x !== s)
}

async function save() {
  const noBatch = items.value.find((r) => gainRow(r) && r.batchEnabled === 1 && !r.batchNo)
  if (noBatch) return uni.showToast({ title: t('wms.check.inputGainBatch', { name: noBatch.materialName }), icon: 'none' })
  const c = check.value
  saving.value = true
  try {
    let id = c.id
    await runAction(
      async () => {
        const payload = {
          warehouseId: c.warehouseId,
          bizDate: c.bizDate,
          remark: c.remark,
          freeze: c.freeze === true || c.freeze === 1,
          items: items.value
            .filter((r) => !r.added || actualOf(r) != null)
            .map((r) => ({
              materialId: r.materialId, locationId: r.locationId || null, batchNo: r.batchNo || null, actualQty: actualOf(r),
              serialNos: r.serialEnabled === 1 && r.serials.length ? r.serials.join(',') : null, remark: r.remark || null,
              // 原样带回已填写的成本价（在电脑端填写的也保留），只对盘盈生效
              costPrice: r.costPrice ?? null
            }))
        }
        if (id) await updateCheck(id, payload)
        else id = await addCheck(payload)
      },
      { success: t('wms.check.saved'), then: () => selectCheck(id) }
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
  &__new-btn {
    margin-top: 16rpx;
  }
  &__search {
    margin: 0 24rpx 16rpx;
    text-align: right;
  }
}
</style>
