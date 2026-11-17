<template>
  <div class="m-page">
    <!-- 选择盘点单 -->
    <template v-if="!check">
      <div class="sub-text">{{ $t('mobile.check.tip') }}</div>
      <!-- 新建盘点：选择仓库后直接扫码 / 搜索物料录入，保存时生成盘点单（草稿） -->
      <div v-if="canAdd" class="m-card new-check">
        <a-select v-model:value="newWarehouseId" :options="warehouses" :placeholder="$t('mobile.check.selectWarehouse')" class="new-warehouse" />
        <a-button type="primary" :disabled="!newWarehouseId" @click="startNew">{{ $t('mobile.check.newCheck') }}</a-button>
      </div>
      <a-spin :spinning="loading">
        <div v-for="c in checks" :key="c.id" class="m-card m-click" @click="selectCheck(c.id)">
          <div class="m-row"><b>{{ c.docNo }}</b><span class="sub-text m-nowrap">{{ c.bizDate }}</span></div>
          <div class="sub-text">{{ c.warehouseName }}{{ c.remark ? ` · ${c.remark}` : '' }}</div>
        </div>
        <a-empty v-if="!loading && !checks.length" :description="$t('mobile.check.noChecks')" />
      </a-spin>
    </template>

    <!-- 按库位扫码盘点 -->
    <template v-else>
      <div class="m-card">
        <div class="m-row"><b>{{ check.docNo || $t('mobile.check.newCheck') }}</b><a @click="leave">{{ $t('mobile.check.change') }}</a></div>
        <div class="sub-text">{{ check.warehouseName }} · {{ $t('mobile.check.counted', { n: countedCount, total: items.length }) }}</div>
      </div>
      <ScanBox ref="scanRef" :placeholder="$t('mobile.common.scanLocThenItem')" @scan="onScan" />
      <MaterialSelect :value="null" :placeholder="$t('mobile.check.searchAdd')" class="search-add" @select="onPick" />
      <div v-if="currentLocation" class="m-current">
        {{ $t('mobile.common.currentLocation') }} <a-tag color="blue">{{ currentLocation.code }}</a-tag>
        <a-checkbox v-model:checked="onlyCurrent">{{ $t('mobile.check.onlyCurrent') }}</a-checkbox>
      </div>
      <div v-for="row in shown" :key="row.key" class="m-card" :class="{ 'm-active': row === activeRow }" @click="activeRow = row">
        <div class="m-row">
          <a-tag color="orange">{{ locationText(row.locationCode) }}</a-tag>
          <span class="sub-text">{{ $t('mobile.check.book', { n: formatQty(row.bookQty) }) }} {{ row.unit }}</span>
        </div>
        <div>
          <b>{{ row.materialCode }} {{ row.materialName }}</b>
          <a-tag v-if="row.added" :color="row.bookQty ? 'blue' : 'green'" class="ml">{{ row.bookQty ? $t('mobile.check.newLine') : $t('mobile.check.gain') }}</a-tag>
        </div>
        <div class="sub-text">{{ row.spec }}{{ row.batchNo && !gainRow(row) ? $t('mobile.common.batchN', { no: row.batchNo }) : '' }}</div>
        <div class="m-grid">
          <a-input v-if="gainRow(row) && row.batchEnabled === 1" v-model:value="row.batchNo" :maxlength="64" :addon-before="$t('wms.common.batch')" />
          <a-input-number v-if="row.serialEnabled !== 1" v-model:value="row.actualQty" :min="0" :addon-before="$t('mobile.check.actual')"
            :class="diffClass(row)" />
        </div>
        <div v-if="row.serialEnabled === 1" class="m-serials">
          <span class="sub-text">{{ $t('mobile.check.actualScan', { n: row.serials.length }) }}</span>
          <a-tag v-for="s in row.serials" :key="s" closable @close="removeSerial(row, s)">{{ s }}</a-tag>
        </div>
      </div>
      <div class="m-footer">
        <span>{{ $t('mobile.check.diff') }} <b>{{ diffCount }}</b></span>
        <a-button type="primary" size="large" :loading="saving" @click="save">{{ $t('mobile.check.save') }}</a-button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import ScanBox from '@/components/mobile/ScanBox.vue'
import { resolveScan } from '@/components/mobile/mobile'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import { addCheck, checkMaterialItems, getCheck, pageCheck, updateCheck } from '@/api/wms/check'
import { locationOptions } from '@/api/wms/location'
import { warehouseOptions } from '@/api/wms/warehouse'
import { useUserStore } from '@/store/user'
import { qty4 } from '@/components/scm/scm'
import { formatQty, locationText, rowKey, splitSerials, today } from '@/components/wms/wms'

import { WmsDocStatus } from '@/constants/enums'
/**
 * 盘点：选择草稿盘点单，扫描库位后扫描物料累计实盘数量（包装条码按换算比例），序列号物料扫描序列号；
 * 没有条码的可搜索物料后录入实盘数量。不在明细中的物料随扫随加（issue #123）：盘点仓库中有库存的按库存位置带出账面数量，
 * 没有库存的作为盘盈行添加；有新增权限时可选择仓库直接开始盘点，第一次保存时生成盘点单（草稿）。保存实盘数量，盘点确认仍在后台进行
 */
const { t } = useI18n()
const scanRef = ref()
const loading = ref(false)
const checks = ref([])
const check = ref()
const items = ref([])
const locations = ref([])
const currentLocation = ref(null)
const onlyCurrent = ref(true)
const activeRow = ref()
const saving = ref(false)

const canAdd = useUserStore().hasPerm('wms:check:add')
const warehouses = ref([])
const newWarehouseId = ref()

onMounted(() => {
  if (canAdd) warehouseOptions().then((list) => (warehouses.value = list.map((w) => ({ value: w.id, label: `${w.name}（${w.code}）`, name: w.name }))))
  loadChecks()
})

async function loadChecks() {
  loading.value = true
  try {
    checks.value = (await pageCheck({ status: WmsDocStatus.DRAFT, current: 1, size: 50 })).records
  } finally {
    loading.value = false
  }
}

async function selectCheck(id) {
  const c = await getCheck(id)
  items.value = c.items.map((i) => ({
    ...i, key: `item-${i.id}`, bookQty: Number(i.bookQty), actualQty: i.actualQty == null ? null : Number(i.actualQty),
    serials: splitSerials(i.serialNos), added: false
  }))
  locations.value = await locationOptions(c.warehouseId)
  check.value = c
  currentLocation.value = null
  activeRow.value = undefined
}
/** 新建盘点：明细为空，扫码 / 搜索物料逐个加入，保存时新增盘点单 */
async function startNew() {
  const w = warehouses.value.find((o) => o.value === newWarehouseId.value)
  items.value = []
  locations.value = await locationOptions(w.value)
  check.value = { warehouseId: w.value, warehouseName: w.name, bizDate: today(), remark: '', freeze: false, items: [] }
  currentLocation.value = null
  activeRow.value = undefined
}

function leave() {
  check.value = undefined
  loadChecks()
}

const shown = computed(() =>
  currentLocation.value && onlyCurrent.value ? items.value.filter((r) => r.locationId === currentLocation.value.id) : items.value
)
const actualOf = (r) => (r.serialEnabled === 1 ? (r.serials.length || r.actualQty != null ? r.serials.length : null) : r.actualQty)
const countedCount = computed(() => items.value.filter((r) => actualOf(r) != null).length)
const diffCount = computed(() => items.value.filter((r) => actualOf(r) != null && qty4(actualOf(r)) !== r.bookQty).length)
const diffClass = (r) => (r.actualQty == null ? '' : qty4(r.actualQty) === r.bookQty ? 'qty-done' : 'qty-short')

async function onScan(code) {
  const scan = scanRef.value
  const hit = await resolveScan(code, locations.value)
  if (hit.type === 'location') {
    currentLocation.value = hit.location
    activeRow.value = undefined
    return scan.show(t('mobile.check.locationLines', { code: hit.location.code, n: shown.value.length }))
  }
  if (hit.type === 'material') {
    const row = await locate(hit.material)
    if (row.serialEnabled === 1) return scan.show(t('mobile.common.scanSerial', { name: row.materialName }))
    row.actualQty = qty4((row.actualQty || 0) + hit.ratio)
    return scan.show(t(gainRow(row) ? 'mobile.check.addedGain' : 'mobile.check.added', { name: row.materialName, qty: formatQty(hit.ratio), actual: formatQty(row.actualQty) }))
  }
  const row = activeRow.value
  if (row?.serialEnabled !== 1) return scan.show(t('mobile.common.unknownCode', { code }), false)
  if (items.value.some((r) => r.serials.includes(code))) return scan.show(t('mobile.common.serialScanned', { code }), false)
  row.serials.push(code)
  scan.show(t('mobile.check.serialAdded', { name: row.materialName, code, n: row.serials.length }))
}
/** 搜索选择物料：定位到（或加入）该物料的明细行，由盘点人录入实盘数量 */
async function onPick(m) {
  if (!m) return
  const row = await locate(m)
  scanRef.value.show(t(row.serialEnabled === 1 ? 'mobile.common.scanSerial' : 'mobile.check.inputActual', { name: row.materialName }))
}

/** 手机上添加、账面为 0 的行（盘盈），需要填写批次号 */
const gainRow = (r) => r.added && !r.bookQty

/**
 * 物料对应的明细行：扫描了库位时找该库位的行，否则找该物料的任一行；明细中没有时加入——
 * 盘点仓库中有库存的按库存位置各加一行（带出账面数量，扫描了库位时只加该库位的），没有库存的加一行账面 0（盘盈）
 */
async function locate(m) {
  const locationId = currentLocation.value?.id || null
  const matches = (r) => r.materialId === m.id && (!locationId || (r.locationId || null) === locationId)
  let list = items.value.filter(matches)
  if (!list.length) {
    const exists = new Set(items.value.map((r) => `${r.locationId || 0}|${r.materialId}|${r.batchNo || ''}`))
    let slots = []
    try {
      slots = await checkMaterialItems({ warehouseId: check.value.warehouseId, materialIds: [m.id] })
    } catch {
      // 取不到库存时按账面 0 添加，保存时后端仍按当时的库存计算账面数量
    }
    const added = slots
      .filter((s) => Number(s.bookQty) !== 0 && (!locationId || (s.locationId || null) === locationId))
      .filter((s) => !exists.has(`${s.locationId || 0}|${s.materialId}|${s.batchNo || ''}`))
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

function removeSerial(row, s) {
  row.serials = row.serials.filter((x) => x !== s)
}

async function save() {
  const noBatch = items.value.find((r) => gainRow(r) && r.batchEnabled === 1 && !r.batchNo)
  if (noBatch) return message.warning(t('mobile.check.inputGainBatch', { name: noBatch.materialName }))
  saving.value = true
  try {
    const c = check.value
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
          // 保留电脑端填写的盘盈成本价
          costPrice: r.costPrice ?? null
        }))
    }
    const id = c.id || (await addCheck(payload))
    if (c.id) await updateCheck(c.id, payload)
    message.success(t('mobile.check.saved'))
    await selectCheck(id)
  } finally {
    saving.value = false
  }
}
</script>

<style scoped src="../mobile.css"></style>
<style scoped>
.ml {
  margin-left: 6px;
}
.new-check {
  display: flex;
  gap: 8px;
}
.new-warehouse {
  flex: 1;
  min-width: 0;
}
.search-add {
  width: 100%;
  margin-bottom: 8px;
}
</style>
