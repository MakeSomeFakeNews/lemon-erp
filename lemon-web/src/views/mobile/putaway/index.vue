<template>
  <div>
    <div class="m-card">
      <WarehouseSelect v-model:value="warehouseId" :placeholder="$t('wms.common.warehouse')" class="full" @update:value="changeWarehouse" />
      <div v-if="source" class="m-row m-current">
        <span>{{ $t('mobile.putaway.source') }} <a-tag color="orange">{{ source.code }}</a-tag></span><a @click="clearSource">{{ $t('mobile.putaway.change') }}</a>
      </div>
    </div>
    <ScanBox ref="scanRef" :placeholder="source ? $t('mobile.putaway.scanItem') : $t('mobile.putaway.scanSource')" @scan="onScan" />
    <a-spin :spinning="loading">
      <div v-for="row in rows" :key="row.key" class="m-card" :class="{ 'm-active': row === activeRow }" @click="activeRow = row">
        <div class="m-row">
          <b>{{ row.materialCode }} {{ row.materialName }}</b>
          <span class="sub-text">{{ $t('wms.common.availableN', { n: formatQty(row.available) }) }} {{ row.unit }}</span>
        </div>
        <div class="sub-text">{{ row.spec }}{{ row.batchNo ? $t('mobile.common.batchN', { no: row.batchNo }) : '' }}</div>
        <div class="m-grid">
          <a-input-number v-model:value="row.qty" :min="0" :max="row.available" :precision="row.serialEnabled === 1 ? 0 : undefined"
            :disabled="row.serialEnabled === 1" :addon-before="$t('mobile.putaway.qty')" />
          <LocationSelect v-model:value="row.toLocationId" :warehouse-id="warehouseId" :placeholder="$t('mobile.putaway.target')" />
        </div>
        <div v-if="row.serialEnabled === 1" class="m-serials">
          <span class="sub-text">{{ $t('mobile.common.selectThenScanSerial') }}</span>
          <a-tag v-for="s in row.serials" :key="s" closable @close="removeSerial(row, s)">{{ s }}</a-tag>
        </div>
      </div>
      <a-empty v-if="source && !loading && !rows.length" :description="$t('mobile.putaway.noStock')" />
    </a-spin>
    <div v-if="rows.length" class="m-footer">
      <a-button size="large" @click="suggest">{{ $t('mobile.putaway.suggest') }}</a-button>
      <a-button type="primary" size="large" :loading="saving" @click="submit">{{ $t('mobile.putaway.submit', { n: moves.length }) }}</a-button>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import ScanBox from '@/components/mobile/ScanBox.vue'
import LocationSelect from '@/components/wms/LocationSelect.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { resolveScan } from '@/components/mobile/mobile'
import { addDoc, confirmDoc } from '@/api/wms/doc'
import { locationOptions } from '@/api/wms/location'
import { availableSerials, pageStock } from '@/api/wms/stock'
import { availableQty, fillPutaway, formatQty, today } from '@/components/wms/wms'
import { useUserStore } from '@/store/user'

/**
 * 上架：扫描来源库位（收货区等）列出库存，扫描物料选中后扫描目标库位，或按上架策略推荐；
 * 生成同仓库的调拨单并确认（没有调拨确认权限时保存为草稿）
 */
const userStore = useUserStore()
const { t } = useI18n()
const scanRef = ref()
const warehouseId = ref()
const locations = ref([])
const source = ref(null)
const rows = ref([])
const activeRow = ref()
const loading = ref(false)
const saving = ref(false)

async function changeWarehouse() {
  clearSource()
  locations.value = warehouseId.value ? await locationOptions(warehouseId.value) : []
}
function clearSource() {
  source.value = null
  rows.value = []
  activeRow.value = undefined
}
async function loadStock() {
  loading.value = true
  try {
    const res = await pageStock({ warehouseId: warehouseId.value, locationId: source.value.id, current: 1, size: 500 })
    rows.value = res.records
      .filter((s) => availableQty(s) > 0)
      .map((s) => ({
        key: s.id, materialId: s.materialId, materialCode: s.materialCode, materialName: s.materialName, spec: s.spec, unit: s.unit,
        serialEnabled: s.serialEnabled, batchNo: s.batchNo, available: availableQty(s), qty: s.serialEnabled === 1 ? 0 : availableQty(s),
        toLocationId: null, serials: [], serialPool: null
      }))
  } finally {
    loading.value = false
  }
}

const moves = computed(() => rows.value.filter((r) => Number(r.qty) > 0 && r.toLocationId))

async function onScan(code) {
  const scan = scanRef.value
  if (!warehouseId.value) return scan.show(t('wms.common.selectWarehouseFirst'), false)
  const hit = await resolveScan(code, locations.value)
  if (hit.type === 'location') {
    if (!source.value) {
      source.value = hit.location
      await loadStock()
      return scan.show(t('mobile.putaway.sourceN', { code: hit.location.code, n: rows.value.length }))
    }
    if (!activeRow.value) return scan.show(t('mobile.putaway.scanItemFirst'), false)
    if (hit.location.id === source.value.id) return scan.show(t('mobile.putaway.sameLocation'), false)
    activeRow.value.toLocationId = hit.location.id
    return scan.show(`${activeRow.value.materialName} → ${hit.location.code}`)
  }
  if (!source.value) return scan.show(t('mobile.putaway.scanSourceFirst'), false)
  if (hit.type === 'material') {
    const row = rows.value.find((r) => r.materialId === hit.material.id && !r.toLocationId) || rows.value.find((r) => r.materialId === hit.material.id)
    if (!row) return scan.show(t('mobile.common.notInLocation', { code: hit.material.code }), false)
    activeRow.value = row
    return scan.show(row.serialEnabled === 1 ? t('mobile.common.scanSerial', { name: row.materialName }) : t('mobile.putaway.selected', { name: row.materialName }))
  }
  const row = activeRow.value
  if (row?.serialEnabled !== 1) return scan.show(t('mobile.common.unknownCode', { code }), false)
  row.serialPool ??= await availableSerials({ warehouseId: warehouseId.value, locationId: source.value.id, materialId: row.materialId,
    batchNo: row.batchNo || undefined })
  if (!row.serialPool.includes(code)) return scan.show(t('mobile.putaway.serialNotHere', { code }), false)
  if (row.serials.includes(code)) return scan.show(t('mobile.common.serialScanned', { code }), false)
  row.serials.push(code)
  row.qty = row.serials.length
  scan.show(t('mobile.common.serialAdded', { name: row.materialName, code, n: row.qty }))
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
  message.info(filled ? t('mobile.putaway.suggested', { n: filled }) : t('mobile.putaway.noSuggestion'))
}

async function submit() {
  if (!moves.value.length) return message.warning(t('mobile.putaway.fillMoves'))
  saving.value = true
  try {
    const id = await addDoc('transfer', {
      warehouseId: warehouseId.value,
      toWarehouseId: warehouseId.value,
      bizDate: today(),
      remark: '移动上架', // i18n-ignore 存入单据备注
      items: moves.value.map((r) => ({
        materialId: r.materialId, locationId: source.value.id, toLocationId: r.toLocationId, batchNo: r.batchNo || null, qty: r.qty,
        serialNos: r.serialEnabled === 1 ? r.serials.join(',') : null
      }))
    })
    if (userStore.hasPerm('wms:transfer:confirm')) {
      await confirmDoc('transfer', id)
      message.success(t('mobile.putaway.done'))
    } else {
      message.success(t('mobile.putaway.pending'))
    }
    await loadStock()
  } finally {
    saving.value = false
  }
}
</script>

<style scoped src="../mobile.css"></style>
<style scoped>
.full {
  width: 100%;
}
</style>
