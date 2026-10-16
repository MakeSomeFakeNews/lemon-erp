<template>
  <div>
    <!-- 选择盘点单 -->
    <template v-if="!check">
      <div class="sub-text">在后台按库存生成盘点单（草稿）后，在这里扫码录入实盘数量</div>
      <a-spin :spinning="loading">
        <div v-for="c in checks" :key="c.id" class="m-card m-click" @click="selectCheck(c.id)">
          <div class="m-row"><b>{{ c.docNo }}</b><span class="sub-text">{{ c.bizDate }}</span></div>
          <div class="sub-text">{{ c.warehouseName }}{{ c.remark ? ` · ${c.remark}` : '' }}</div>
        </div>
        <a-empty v-if="!loading && !checks.length" description="没有待盘点的盘点单" />
      </a-spin>
    </template>

    <!-- 按库位扫码盘点 -->
    <template v-else>
      <div class="m-card">
        <div class="m-row"><b>{{ check.docNo }}</b><a @click="leave">换盘点单</a></div>
        <div class="sub-text">{{ check.warehouseName }} · 已盘 {{ countedCount }} / {{ items.length }} 行</div>
      </div>
      <ScanBox ref="scanRef" placeholder="扫描库位，再扫描物料 / 序列号" @scan="onScan" />
      <div v-if="currentLocation" class="m-current">
        当前库位 <a-tag color="blue">{{ currentLocation.code }}</a-tag>
        <a-checkbox v-model:checked="onlyCurrent">只看当前库位</a-checkbox>
      </div>
      <div v-for="row in shown" :key="row.key" class="m-card" :class="{ 'm-active': row === activeRow }" @click="activeRow = row">
        <div class="m-row">
          <a-tag color="orange">{{ locationText(row.locationCode) }}</a-tag>
          <span class="sub-text">账面 {{ formatQty(row.bookQty) }} {{ row.unit }}</span>
        </div>
        <div>
          <b>{{ row.materialCode }} {{ row.materialName }}</b>
          <a-tag v-if="row.added" color="green" class="ml">盘盈</a-tag>
        </div>
        <div class="sub-text">{{ row.spec }}{{ row.batchNo && !row.added ? ` 批次 ${row.batchNo}` : '' }}</div>
        <div class="m-grid">
          <a-input v-if="row.added && row.batchEnabled === 1" v-model:value="row.batchNo" :maxlength="64" addon-before="批次" />
          <a-input-number v-if="row.serialEnabled !== 1" v-model:value="row.actualQty" :min="0" addon-before="实盘"
            :class="diffClass(row)" />
        </div>
        <div v-if="row.serialEnabled === 1" class="m-serials">
          <span class="sub-text">实盘 {{ row.serials.length }}，扫描序列号：</span>
          <a-tag v-for="s in row.serials" :key="s" closable @close="removeSerial(row, s)">{{ s }}</a-tag>
        </div>
      </div>
      <div class="m-footer">
        <span>差异 <b>{{ diffCount }}</b> 行</span>
        <a-button type="primary" size="large" :loading="saving" @click="save">保存实盘</a-button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import ScanBox from '@/components/mobile/ScanBox.vue'
import { resolveScan } from '@/components/mobile/mobile'
import { getCheck, pageCheck, updateCheck } from '@/api/wms/check'
import { locationOptions } from '@/api/wms/location'
import { qty4 } from '@/components/scm/scm'
import { formatQty, locationText, rowKey, splitSerials } from '@/components/wms/wms'

/**
 * 盘点：选择草稿盘点单，扫描库位后扫描物料累计实盘数量（包装条码按换算比例），序列号物料扫描序列号；
 * 账面没有的物料作为盘盈行添加。保存实盘数量，盘点确认仍在后台进行
 */
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

onMounted(async () => {
  loading.value = true
  try {
    checks.value = (await pageCheck({ status: 'DRAFT', current: 1, size: 50 })).records
  } finally {
    loading.value = false
  }
})

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
function leave() {
  check.value = undefined
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
    return scan.show(`库位 ${hit.location.code}，${shown.value.length} 行`)
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
    }
    activeRow.value = row
    if (row.serialEnabled === 1) return scan.show(`${row.materialName}：请扫描序列号`)
    row.actualQty = qty4((row.actualQty || 0) + hit.ratio)
    return scan.show(`${row.materialName}${row.added ? '（盘盈）' : ''} +${formatQty(hit.ratio)}，实盘 ${formatQty(row.actualQty)}`)
  }
  const row = activeRow.value
  if (row?.serialEnabled !== 1) return scan.show(`未识别的条码 ${code}`, false)
  if (items.value.some((r) => r.serials.includes(code))) return scan.show(`序列号 ${code} 已扫描`, false)
  row.serials.push(code)
  scan.show(`${row.materialName} 序列号 ${code}，实盘 ${row.serials.length}`)
}
function removeSerial(row, s) {
  row.serials = row.serials.filter((x) => x !== s)
}

async function save() {
  const noBatch = items.value.find((r) => r.added && r.batchEnabled === 1 && !r.batchNo)
  if (noBatch) return message.warning(`${noBatch.materialName}：请填写盘盈的批次号`)
  saving.value = true
  try {
    const c = check.value
    await updateCheck(c.id, {
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
    })
    message.success('已保存实盘数量')
    await selectCheck(c.id)
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
</style>
