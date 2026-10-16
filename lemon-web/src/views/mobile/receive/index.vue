<template>
  <div>
    <!-- 选择采购订单 -->
    <template v-if="!order">
      <ScanBox ref="scanRef" placeholder="扫描或输入采购订单号" @scan="searchOrders" />
      <a-spin :spinning="loading">
        <div v-for="o in orders" :key="o.id" class="m-card m-click" @click="selectOrder(o.id)">
          <div class="m-row"><b>{{ o.orderNo }}</b><DictTag type="scm_order_status" :value="o.status" /></div>
          <div class="m-row sub-text"><span>{{ o.partnerName }}</span><span>{{ o.orderDate }}</span></div>
        </div>
        <a-empty v-if="!loading && !orders.length" description="没有待入库的采购订单" />
      </a-spin>
    </template>

    <!-- 扫码收货 -->
    <template v-else>
      <div class="m-card">
        <div class="m-row"><b>{{ order.orderNo }}</b><a @click="reset">换订单</a></div>
        <div class="sub-text">{{ order.partnerName }}</div>
        <WarehouseSelect v-model:value="warehouseId" class="m-field" placeholder="收货仓库" @update:value="loadLocations" />
      </div>
      <ScanBox ref="scanRef" placeholder="扫描物料或库位条码" @scan="onScan" />
      <div v-if="currentLocation" class="m-current">
        当前库位 <a-tag color="blue">{{ currentLocation.code }}</a-tag><a @click="currentLocation = null">清除</a>
      </div>
      <div v-for="row in rows" :key="row.orderItemId" class="m-card" :class="{ 'm-active': row === activeRow }" @click="activeRow = row">
        <div class="m-row">
          <b>{{ row.materialCode }} {{ row.materialName }}</b>
          <span :class="row.qty >= row.limit ? 'qty-done' : ''">{{ formatQty(row.qty) }} / {{ formatQty(row.limit) }} {{ row.unit }}</span>
        </div>
        <div v-if="row.spec" class="sub-text">{{ row.spec }}</div>
        <div class="m-grid">
          <a-input-number v-model:value="row.qty" :min="0" :max="row.limit" :precision="row.serialEnabled === 1 ? 0 : undefined" :disabled="row.serialEnabled === 1"
            addon-before="数量" />
          <LocationSelect v-model:value="row.locationId" :warehouse-id="warehouseId" placeholder="库位（空则推荐）" />
          <a-input v-if="row.batchEnabled === 1" v-model:value="row.batchNo" :maxlength="64" addon-before="批次" />
        </div>
        <div v-if="row.serialEnabled === 1" class="m-serials">
          <span class="sub-text">选中后扫描序列号：</span>
          <a-tag v-for="s in row.serials" :key="s" closable @close="removeSerial(row, s)">{{ s }}</a-tag>
        </div>
      </div>
      <div class="m-footer">
        <span>本次收货 <b>{{ formatQty(totalQty) }}</b></span>
        <a-button type="primary" size="large" :loading="saving" :disabled="!totalQty" @click="submit">确认入库</a-button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import DictTag from '@/components/DictTag.vue'
import ScanBox from '@/components/mobile/ScanBox.vue'
import LocationSelect from '@/components/wms/LocationSelect.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { resolveScan } from '@/components/mobile/mobile'
import { getOrder, pageOrder } from '@/api/scm/order'
import { addTrade } from '@/api/scm/trade'
import { locationOptions } from '@/api/wms/location'
import { openQty, qty4 } from '@/components/scm/scm'
import { fillPutaway, formatQty, today } from '@/components/wms/wms'
import { useDict } from '@/composables/useDict'

/**
 * 扫码收货：选择采购订单后逐个扫描物料条码累计数量（包装条码按换算比例），
 * 序列号物料选中后扫描序列号；没有指定库位的按上架策略推荐，生成采购入库单
 */
useDict('scm_order_status')
const scanRef = ref()
const loading = ref(false)
const orders = ref([])
const order = ref()
const warehouseId = ref()
const locations = ref([])
const currentLocation = ref(null)
const rows = ref([])
const activeRow = ref()
const saving = ref(false)

async function searchOrders(orderNo) {
  loading.value = true
  try {
    const res = await pageOrder('purchase', { orderNo: orderNo || undefined, current: 1, size: 50 })
    orders.value = res.records.filter((o) => ['APPROVED', 'PARTIAL'].includes(o.status))
    if (orderNo && orders.value.length === 1) selectOrder(orders.value[0].id)
  } finally {
    loading.value = false
  }
}
onMounted(() => searchOrders())

async function selectOrder(id) {
  const o = await getOrder('purchase', id)
  rows.value = o.items
    .filter((i) => openQty(i) > 0)
    .map((i) => ({
      orderItemId: i.id, materialId: i.materialId, materialCode: i.materialCode, materialName: i.materialName, spec: i.spec, unit: i.unit,
      batchEnabled: i.batchEnabled, serialEnabled: i.serialEnabled, limit: openQty(i), qty: 0, locationId: null, batchNo: '', serials: []
    }))
  if (!rows.value.length) return message.warning('订单没有待入库的明细')
  order.value = o
  warehouseId.value = o.warehouseId || undefined
  activeRow.value = undefined
  currentLocation.value = null
  loadLocations()
}
async function loadLocations() {
  locations.value = warehouseId.value ? await locationOptions(warehouseId.value) : []
  currentLocation.value = null
}
function reset() {
  order.value = undefined
  searchOrders()
}

const totalQty = computed(() => qty4(rows.value.reduce((s, r) => s + (Number(r.qty) || 0), 0)))

async function onScan(code) {
  const scan = scanRef.value
  const hit = await resolveScan(code, locations.value)
  if (hit.type === 'location') {
    currentLocation.value = hit.location
    if (activeRow.value && !activeRow.value.locationId) activeRow.value.locationId = hit.location.id
    return scan.show(`当前库位 ${hit.location.code}`)
  }
  if (hit.type === 'material') {
    const candidates = rows.value.filter((r) => r.materialId === hit.material.id)
    if (!candidates.length) return scan.show(`${hit.material.code} 不在该订单中`, false)
    const row = candidates.find((r) => r.qty < r.limit) || candidates[0]
    activeRow.value = row
    if (currentLocation.value && !row.locationId) row.locationId = currentLocation.value.id
    if (row.serialEnabled === 1) return scan.show(`${row.materialName}：请扫描序列号`)
    if (qty4(row.qty + hit.ratio) > row.limit) return scan.show(`${row.materialName} 已超过待入库数量`, false)
    row.qty = qty4(row.qty + hit.ratio)
    return scan.show(`${row.materialName} +${formatQty(hit.ratio)} ${row.unit}${hit.ratio !== 1 ? `（1 ${hit.unit}）` : ''}，共 ${formatQty(row.qty)}`)
  }
  // 其他条码：作为当前序列号物料的序列号
  const row = activeRow.value
  if (row?.serialEnabled !== 1) return scan.show(`未识别的条码 ${code}`, false)
  if (row.serials.includes(code)) return scan.show(`序列号 ${code} 已扫描`, false)
  if (row.serials.length >= row.limit) return scan.show(`${row.materialName} 已超过待入库数量`, false)
  row.serials.push(code)
  row.qty = row.serials.length
  scan.show(`${row.materialName} 序列号 ${code}，共 ${row.qty}`)
}
function removeSerial(row, s) {
  row.serials = row.serials.filter((x) => x !== s)
  row.qty = row.serials.length
}

async function submit() {
  if (!warehouseId.value) return message.warning('请选择收货仓库')
  const list = rows.value.filter((r) => Number(r.qty) > 0)
  const noBatch = list.find((r) => r.batchEnabled === 1 && !r.batchNo)
  if (noBatch) return message.warning(`${noBatch.materialName}：请填写批次号`)
  saving.value = true
  try {
    await fillPutaway(warehouseId.value, list)
    await addTrade('purchaseIn', {
      orderId: order.value.id,
      warehouseId: warehouseId.value,
      bizDate: today(),
      items: list.map((r) => ({
        orderItemId: r.orderItemId, locationId: r.locationId || null, batchNo: r.batchNo || null, qty: r.qty,
        serialNos: r.serialEnabled === 1 ? r.serials.join(',') : null
      }))
    })
    message.success('已入库')
    reset()
  } finally {
    saving.value = false
  }
}
</script>

<style scoped src="../mobile.css"></style>
