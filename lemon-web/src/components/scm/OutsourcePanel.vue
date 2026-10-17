<template>
  <a-spin :spinning="loading">
    <a-empty v-if="!loading && !list.length" :description="$t('scm.outsource.empty')" />
    <template v-else>
      <div class="ops">
        <span class="sub-text">{{ $t('scm.outsource.tip') }}</span>
        <a-space v-if="editable">
          <a-button size="small" type="primary" @click="openIssue">{{ $t('scm.outsource.issue') }}</a-button>
          <a-button size="small" @click="openReturn">{{ $t('scm.outsource.return') }}</a-button>
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
            <div v-if="atSupplier(record) < 0" class="sub-text danger">{{ $t('scm.outsource.overConsumed') }}</div>
          </template>
          <template v-else-if="column.qty">{{ formatQty(record[column.dataIndex]) }}</template>
        </template>
      </a-table>
    </template>

    <a-modal v-model:open="open" :title="mode === 'issue' ? $t('scm.outsource.issue') : $t('scm.outsource.return')" width="min(760px, 100vw)" :confirm-loading="saving" destroy-on-close @ok="submit">
      <a-form layout="inline" class="head">
        <a-form-item :label="$t('scm.order.warehouse')" required>
          <WarehouseSelect v-model:value="form.warehouseId" style="width: 180px" @change="form.items = []" />
        </a-form-item>
        <a-form-item :label="$t('common.field.date')" required>
          <a-date-picker v-model:value="form.bizDate" value-format="YYYY-MM-DD" />
        </a-form-item>
        <a-button v-if="mode === 'issue'" :disabled="!form.warehouseId" @click="pickerOpen = true">{{ $t('scm.outsource.pickStock') }}</a-button>
      </a-form>
      <a-table row-key="key" size="small" :columns="formColumns" :data-source="form.items" :pagination="false">
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'material'">{{ record.materialCode }} {{ record.materialName }}</template>
          <template v-else-if="column.key === 'slot'">
            <template v-if="mode === 'issue'">{{ record.locationCode || $t('scm.stockSlot.unspecified') }}{{ record.batchNo ? ' · ' + record.batchNo : '' }}</template>
            <a-input v-else-if="record.batchEnabled === 1" v-model:value="record.batchNo" size="small" :placeholder="$t('scm.outsource.batchNo')" />
          </template>
          <template v-else-if="column.key === 'qty'">
            <a-input-number v-model:value="record.qty" :min="0.0001" :max="record.max" size="small" />
            <span class="sub-text"> / {{ formatQty(record.max) }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" danger size="small" @click="form.items.splice(index, 1)">{{ $t('common.action.delete') }}</a-button>
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
import { useI18n } from 'vue-i18n'
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
const { t } = useI18n()

const columns = [
  { title: t('scm.outsource.product'), key: 'product', width: 180 },
  { title: t('scm.outsource.component'), key: 'material', width: 200 },
  { title: t('scm.outsource.unitQty'), dataIndex: 'unitQty', width: 100, align: 'right', qty: true },
  { title: t('scm.outsource.required'), dataIndex: 'requiredQty', width: 100, align: 'right', qty: true },
  { title: t('scm.outsource.issued'), dataIndex: 'issuedQty', width: 90, align: 'right', qty: true },
  { title: t('scm.outsource.returned'), dataIndex: 'returnedQty', width: 100, align: 'right', qty: true },
  { title: t('scm.outsource.consumed'), dataIndex: 'consumedQty', width: 100, align: 'right', qty: true },
  { title: t('scm.outsource.atSupplier'), key: 'atSupplier', width: 140, align: 'right' }
]
const formColumns = [
  { title: t('scm.order.material'), key: 'material', width: 220 },
  { title: t('scm.consign.slot'), key: 'slot', width: 160 },
  { title: t('common.field.quantity'), key: 'qty', width: 180 },
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
    message.info(t('scm.outsource.noBalance'))
    return
  }
  form.value = { warehouseId: undefined, bizDate: today(), items: [...byMaterial.values()] }
  open.value = true
}
function onPick(stocks) {
  const allowed = new Set(list.value.map((r) => r.materialId))
  stocks.forEach((s) => {
    if (!allowed.has(s.materialId)) {
      message.warning(t('scm.outsource.notMaterial', { code: s.materialCode }))
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
    message.warning(t('scm.outsource.selectRequired'))
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
    message.success(mode.value === 'issue' ? t('scm.outsource.issuedMsg') : t('scm.outsource.returnedMsg'))
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
