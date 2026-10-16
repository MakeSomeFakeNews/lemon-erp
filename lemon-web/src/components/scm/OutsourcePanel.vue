<template>
  <a-spin :spinning="loading">
    <a-empty v-if="!loading && !list.length" description="订单中没有委外件（物料属性为委外且有默认 BOM 的物料）" />
    <template v-else>
      <div class="ops">
        <span class="sub-text">加工商处结存 = 已发 - 已退 - 已核销；委外件收货时按单位用量自动核销用料，成本核算把核销的子件成本转入委外件</span>
        <a-space v-if="editable">
          <a-button size="small" type="primary" @click="openIssue">委外发料</a-button>
          <a-button size="small" @click="openReturn">委外退料</a-button>
        </a-space>
      </div>
      <a-table row-key="id" size="small" :columns="columns" :data-source="list" :pagination="false" :scroll="{ x: 900 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'product'">{{ record.productCode }} {{ record.productName }}</template>
          <template v-else-if="column.key === 'material'">
            <div>{{ record.materialCode }} {{ record.materialName }}</div>
            <div class="sub-text">{{ record.spec }}</div>
          </template>
          <template v-else-if="column.key === 'atSupplier'">
            <span :class="{ danger: atSupplier(record) < 0 }">{{ formatQty(atSupplier(record)) }}</span>
            <div v-if="atSupplier(record) < 0" class="sub-text danger">超耗（核销多于实发）</div>
          </template>
          <template v-else-if="column.qty">{{ formatQty(record[column.dataIndex]) }}</template>
        </template>
      </a-table>
    </template>

    <a-modal v-model:open="open" :title="mode === 'issue' ? '委外发料' : '委外退料'" width="min(760px, 100vw)" :confirm-loading="saving" destroy-on-close @ok="submit">
      <a-form layout="inline" class="head">
        <a-form-item label="仓库" required>
          <WarehouseSelect v-model:value="form.warehouseId" style="width: 180px" @change="form.items = []" />
        </a-form-item>
        <a-form-item label="日期" required>
          <a-date-picker v-model:value="form.bizDate" value-format="YYYY-MM-DD" />
        </a-form-item>
        <a-button v-if="mode === 'issue'" :disabled="!form.warehouseId" @click="pickerOpen = true">选择库存</a-button>
      </a-form>
      <a-table row-key="key" size="small" :columns="formColumns" :data-source="form.items" :pagination="false">
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'material'">{{ record.materialCode }} {{ record.materialName }}</template>
          <template v-else-if="column.key === 'slot'">
            <template v-if="mode === 'issue'">{{ record.locationCode || '未指定' }}{{ record.batchNo ? ' · ' + record.batchNo : '' }}</template>
            <a-input v-else-if="record.batchEnabled === 1" v-model:value="record.batchNo" size="small" placeholder="批次号" />
          </template>
          <template v-else-if="column.key === 'qty'">
            <a-input-number v-model:value="record.qty" :min="0.0001" :max="record.max" size="small" />
            <span class="sub-text"> / {{ formatQty(record.max) }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" danger size="small" @click="form.items.splice(index, 1)">删除</a-button>
          </template>
        </template>
      </a-table>
      <StockPicker v-model:open="pickerOpen" :warehouse-id="form.warehouseId" @pick="onPick" />
    </a-modal>
  </a-spin>
</template>

<script setup>
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import StockPicker from '@/components/wms/StockPicker.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { outsourceIssue, outsourceMaterials, outsourceReturn } from '@/api/scm/order'
import { availableQty, formatQty, rowKey, today } from '@/components/wms/wms'

/** 委外订单的用料：发料给加工商、加工商退料，收货核销 */
const props = defineProps({
  orderId: { type: Number, required: true },
  editable: Boolean
})
const emit = defineEmits(['changed'])

const columns = [
  { title: '委外件', key: 'product', width: 180 },
  { title: '子件', key: 'material', width: 200 },
  { title: '单位用量', dataIndex: 'unitQty', width: 90, align: 'right', qty: true },
  { title: '需求', dataIndex: 'requiredQty', width: 90, align: 'right', qty: true },
  { title: '已发', dataIndex: 'issuedQty', width: 90, align: 'right', qty: true },
  { title: '已退', dataIndex: 'returnedQty', width: 90, align: 'right', qty: true },
  { title: '已核销', dataIndex: 'consumedQty', width: 90, align: 'right', qty: true },
  { title: '加工商结存', key: 'atSupplier', width: 120, align: 'right' }
]
const formColumns = [
  { title: '物料', key: 'material', width: 220 },
  { title: '库位 / 批次', key: 'slot', width: 160 },
  { title: '数量', key: 'qty', width: 180 },
  { title: '', key: 'action', width: 60 }
]
const atSupplier = (r) => Number(r.issuedQty) - Number(r.returnedQty) - Number(r.consumedQty)

const list = ref([])
const loading = ref(false)
async function load() {
  loading.value = true
  try {
    list.value = await outsourceMaterials(props.orderId)
  } finally {
    loading.value = false
  }
}
watch(() => props.orderId, load, { immediate: true })

const open = ref(false)
const pickerOpen = ref(false)
const saving = ref(false)
const mode = ref('issue')
const form = ref({})
function openIssue() {
  mode.value = 'issue'
  form.value = { warehouseId: undefined, bizDate: today(), items: [] }
  open.value = true
}
function openReturn() {
  mode.value = 'return'
  // 退料：加工商处有结存的子件，按物料合并
  const byMaterial = new Map()
  list.value.forEach((r) => {
    const left = atSupplier(r)
    if (left <= 0) return
    const e = byMaterial.get(r.materialId) || { key: rowKey(), materialId: r.materialId, materialCode: r.materialCode,
      materialName: r.materialName, batchEnabled: r.batchEnabled, batchNo: '', qty: 0, max: 0 }
    e.max += left
    e.qty = e.max
    byMaterial.set(r.materialId, e)
  })
  if (!byMaterial.size) {
    message.info('加工商处没有结存的物料')
    return
  }
  form.value = { warehouseId: undefined, bizDate: today(), items: [...byMaterial.values()] }
  open.value = true
}
function onPick(stocks) {
  const allowed = new Set(list.value.map((r) => r.materialId))
  stocks.forEach((s) => {
    if (!allowed.has(s.materialId)) {
      message.warning(`${s.materialCode} 不是委外用料`)
      return
    }
    if (form.value.items.some((i) => i.key === s.id)) return
    form.value.items.push({ key: s.id, materialId: s.materialId, materialCode: s.materialCode, materialName: s.materialName,
      locationId: s.locationId || null, locationCode: s.locationCode, batchNo: s.batchNo || null, max: availableQty(s), qty: 1 })
  })
}
async function submit() {
  const f = form.value
  if (!f.warehouseId || !f.items.length) {
    message.warning('请选择仓库和物料')
    return
  }
  saving.value = true
  try {
    const data = {
      warehouseId: f.warehouseId,
      bizDate: f.bizDate,
      items: f.items.map((i) => ({ materialId: i.materialId, locationId: i.locationId || null, batchNo: i.batchNo || null, qty: i.qty }))
    }
    mode.value === 'issue' ? await outsourceIssue(props.orderId, data) : await outsourceReturn(props.orderId, data)
    message.success(mode.value === 'issue' ? '已发料' : '已退料')
    open.value = false
    load()
    emit('changed')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.ops {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.head {
  margin-bottom: 12px;
  row-gap: 8px;
}
.danger {
  color: #ff4d4f;
}
</style>
