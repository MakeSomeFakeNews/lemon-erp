<template>
  <div>
    <!-- 选择发货通知 -->
    <template v-if="!detail">
      <ScanBox ref="scanRef" placeholder="扫描或输入发货通知号" @scan="searchDeliveries" />
      <a-spin :spinning="loading">
        <div v-for="d in deliveries" :key="d.id" class="m-card m-click" @click="selectDelivery(d.id)">
          <div class="m-row"><b>{{ d.deliveryNo }}</b><a-tag v-if="d.waveNo" color="blue">{{ d.waveNo }}</a-tag></div>
          <div class="m-row sub-text"><span>{{ d.customerName }} · {{ d.warehouseName }}</span><span>{{ d.shipDate }}</span></div>
        </div>
        <a-empty v-if="!loading && !deliveries.length" description="没有待拣货的发货通知" />
      </a-spin>
    </template>

    <!-- 按库位顺序拣货 -->
    <template v-else>
      <div class="m-card">
        <div class="m-row"><b>{{ detail.deliveryNo }}</b><a @click="reset">换通知</a></div>
        <div class="sub-text">{{ detail.customerName }} · {{ detail.warehouseName }}</div>
        <a-alert v-if="shortItems.length" type="warning" show-icon class="m-current"
          :message="`${shortItems.map((i) => i.materialName).join('、')} 拣货建议不足，请在后台调整后再拣货`" />
      </div>
      <ScanBox ref="scanRef" placeholder="扫描库位，再扫描物料 / 序列号" @scan="onScan" />
      <div v-if="currentLocation" class="m-current">当前库位 <a-tag color="blue">{{ currentLocation.code }}</a-tag></div>
      <div v-for="row in rows" :key="row.key" class="m-card" :class="{ 'm-active': row === activeRow }" @click="activeRow = row">
        <div class="m-row">
          <a-tag color="orange">{{ locationText(row.locationCode) }}</a-tag>
          <span :class="row.picked >= row.qty ? 'qty-done' : row.picked > 0 ? 'qty-short' : ''">
            {{ formatQty(row.picked) }} / {{ formatQty(row.qty) }} {{ row.unit }}
          </span>
        </div>
        <div><b>{{ row.materialCode }} {{ row.materialName }}</b></div>
        <div class="sub-text">{{ row.spec }}{{ row.batchNo ? ` 批次 ${row.batchNo}` : '' }}</div>
        <a-input-number v-if="row.serialEnabled !== 1" v-model:value="row.picked" :min="0" :max="row.qty" addon-before="已拣" class="m-field" />
        <div v-else class="m-serials">
          <span class="sub-text">扫描序列号：</span>
          <a-tag v-for="s in row.serials" :key="s" closable @close="removeSerial(row, s)">{{ s }}</a-tag>
        </div>
      </div>
      <div class="m-footer">
        <span>已拣 <b>{{ doneCount }}</b> / {{ rows.length }} 行</span>
        <a-button type="primary" size="large" :loading="saving" :disabled="!!shortItems.length" @click="submit">确认拣货</a-button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import ScanBox from '@/components/mobile/ScanBox.vue'
import { resolveScan } from '@/components/mobile/mobile'
import { getDelivery, pageDelivery, pickDelivery } from '@/api/scm/planning'
import { locationOptions } from '@/api/wms/location'
import { availableSerials } from '@/api/wms/stock'
import { qty4 } from '@/components/scm/scm'
import { formatQty, locationText } from '@/components/wms/wms'

/**
 * 拣货：按拣货建议（先到期先出）以库位顺序逐行拣货，扫描库位后扫描物料累计数量，序列号物料扫描序列号；
 * 全部拣完后确认拣货，之后在后台出库
 */
const scanRef = ref()
const loading = ref(false)
const deliveries = ref([])
const detail = ref()
const locations = ref([])
const currentLocation = ref(null)
const rows = ref([])
const activeRow = ref()
const saving = ref(false)

async function searchDeliveries(keyword) {
  loading.value = true
  try {
    const res = await pageDelivery({ status: 'PENDING', keyword: keyword || undefined, current: 1, size: 50 })
    deliveries.value = res.records
    if (keyword && res.records.length === 1) selectDelivery(res.records[0].id)
  } finally {
    loading.value = false
  }
}
onMounted(() => searchDeliveries())

async function selectDelivery(id) {
  const d = await getDelivery(id)
  const items = new Map(d.items.map((i) => [i.id, i]))
  rows.value = (d.picks || [])
    .map((p) => {
      const item = items.get(p.deliveryItemId)
      return {
        key: p.id, deliveryItemId: p.deliveryItemId, materialId: item.materialId, materialCode: item.materialCode, materialName: item.materialName,
        spec: item.spec, unit: item.unit, serialEnabled: item.serialEnabled, locationId: p.locationId, locationCode: p.locationCode,
        batchNo: p.batchNo, qty: Number(p.qty), picked: 0, serials: [], serialPool: null
      }
    })
    .sort((a, b) => String(a.locationCode || '').localeCompare(String(b.locationCode || '')) || a.deliveryItemId - b.deliveryItemId)
  detail.value = d
  currentLocation.value = null
  activeRow.value = undefined
  locations.value = await locationOptions(d.warehouseId)
}
function reset() {
  detail.value = undefined
  searchDeliveries()
}

/** 拣货建议合计少于通知数量的明细（库存不足） */
const shortItems = computed(() =>
  (detail.value?.items || []).filter((i) => qty4(rows.value.filter((r) => r.deliveryItemId === i.id).reduce((s, r) => s + r.qty, 0)) < Number(i.qty))
)
const doneCount = computed(() => rows.value.filter((r) => r.picked >= r.qty).length)

async function onScan(code) {
  const scan = scanRef.value
  const hit = await resolveScan(code, locations.value)
  if (hit.type === 'location') {
    currentLocation.value = hit.location
    const next = rows.value.find((r) => r.locationId === hit.location.id && r.picked < r.qty)
    if (!next) return scan.show(`${hit.location.code} 没有待拣的物料`, false)
    activeRow.value = next
    return scan.show(`库位 ${hit.location.code}`)
  }
  if (hit.type === 'material') {
    const list = rows.value.filter((r) => r.materialId === hit.material.id && r.picked < r.qty)
    if (!list.length) return scan.show(`${hit.material.code} 不需要拣货或已拣完`, false)
    const row = currentLocation.value ? list.find((r) => r.locationId === currentLocation.value.id) : list.find((r) => !r.locationId)
    if (!row) return scan.show(`请到库位 ${list.map((r) => locationText(r.locationCode)).join('、')} 拣货`, false)
    activeRow.value = row
    if (row.serialEnabled === 1) return scan.show(`${row.materialName}：请扫描序列号`)
    if (qty4(row.picked + hit.ratio) > row.qty) return scan.show(`${row.materialName} 超过应拣数量`, false)
    row.picked = qty4(row.picked + hit.ratio)
    return scan.show(`${row.materialName} +${formatQty(hit.ratio)}，${formatQty(row.picked)} / ${formatQty(row.qty)}`)
  }
  const row = activeRow.value
  if (row?.serialEnabled !== 1) return scan.show(`未识别的条码 ${code}`, false)
  row.serialPool ??= await availableSerials({ warehouseId: detail.value.warehouseId, locationId: row.locationId || undefined,
    materialId: row.materialId, batchNo: row.batchNo || undefined })
  if (!row.serialPool.includes(code)) return scan.show(`序列号 ${code} 不在该库位 / 批次`, false)
  if (rows.value.some((r) => r.serials.includes(code))) return scan.show(`序列号 ${code} 已扫描`, false)
  if (row.serials.length >= row.qty) return scan.show(`${row.materialName} 超过应拣数量`, false)
  row.serials.push(code)
  row.picked = row.serials.length
  scan.show(`${row.materialName} 序列号 ${code}，${row.picked} / ${row.qty}`)
}
function removeSerial(row, s) {
  row.serials = row.serials.filter((x) => x !== s)
  row.picked = row.serials.length
}

async function submit() {
  const left = rows.value.find((r) => qty4(Number(r.picked) || 0) !== r.qty)
  if (left) return message.warning(`${locationText(left.locationCode)} ${left.materialName}：已拣 ${formatQty(left.picked)}，应拣 ${formatQty(left.qty)}`)
  saving.value = true
  try {
    await pickDelivery(detail.value.id, {
      picks: rows.value.map((r) => ({
        deliveryItemId: r.deliveryItemId, locationId: r.locationId || null, batchNo: r.batchNo || null, qty: r.qty,
        serialNos: r.serialEnabled === 1 ? r.serials.join(',') : null
      }))
    })
    message.success('拣货完成')
    reset()
  } finally {
    saving.value = false
  }
}
</script>

<style scoped src="../mobile.css"></style>
