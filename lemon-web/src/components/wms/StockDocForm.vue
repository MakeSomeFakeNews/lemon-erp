<template>
  <FormDrawer page :drawer="drawer" :create-title="`新增${conf.label}`" :edit-title="`编辑${conf.label}`" width="min(1280px, 100vw)">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="16">
        <a-col v-if="conf.typeDict" :xs="24" :sm="12" :lg="6">
          <a-form-item :label="conf.typeLabel" name="bizType">
            <a-select
              :value="form.bizType"
              :placeholder="`请选择${conf.typeLabel}`"
              :options="manualTypeOptions"
              @change="onBizTypeChange"
            />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="conf.warehouseLabel" name="warehouseId">
            <WarehouseSelect
              :value="form.warehouseId"
              :allow-clear="false"
              :types="consignIn ? ['CONSIGN_IN'] : undefined"
              :placeholder="consignIn ? '请选择供应商寄售仓' : '请选择仓库'"
              @update:value="onWarehouseChange"
            />
          </a-form-item>
        </a-col>
        <a-col v-if="kind === 'transfer'" :xs="24" :sm="12" :lg="6">
          <a-form-item label="调入仓库" name="toWarehouseId">
            <WarehouseSelect :value="form.toWarehouseId" :allow-clear="false" @update:value="onToWarehouseChange" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item label="业务日期" name="bizDate">
            <a-date-picker v-model:value="form.bizDate" value-format="YYYY-MM-DD" class="full" />
          </a-form-item>
        </a-col>
        <a-col v-if="kind === 'transfer'" :xs="24" :sm="12" :lg="6">
          <a-form-item label="在途调拨" name="inTransit" extra="确认时只调出，调入仓库收货后才入库">
            <a-switch v-model:checked="form.inTransit" checked-children="在途" un-checked-children="直接调拨" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="备注" name="remark">
            <a-input v-model:value="form.remark" :maxlength="255" placeholder="选填" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="items-head">
        <span class="section-title">明细（{{ form.items.length }} 行，合计 {{ formatQty(totalQty) }}）</span>
        <a-space>
          <a-button v-if="kind !== 'out'" :disabled="!(kind === 'in' ? form.warehouseId : form.toWarehouseId)" @click="putaway">
            推荐库位
          </a-button>
          <a-button v-if="kind === 'in'" type="primary" ghost @click="addRow">
            <template #icon><PlusOutlined /></template>添加物料
          </a-button>
          <a-button v-else type="primary" ghost :disabled="!form.warehouseId" @click="pickerOpen = true">
            <template #icon><PlusOutlined /></template>从库存选择
          </a-button>
        </a-space>
      </div>

      <a-table
        row-key="key"
        size="small"
        class="items-table"
        :columns="columns"
        :data-source="form.items"
        :pagination="false"
        :scroll="{ x: scrollX }"
        :locale="{ emptyText: kind === 'in' ? '点击「添加物料」录入明细' : '选择仓库后点击「从库存选择」' }"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'material'">
            <a-form-item v-if="kind === 'in'" :name="['items', index, 'materialId']" :rules="requiredRule('请选择物料')" class="cell-item">
              <MaterialSelect v-model:value="record.materialId" :label="record.materialLabel" @select="(m) => applyMaterial(record, m)" />
            </a-form-item>
            <template v-else>
              <div>{{ record.materialCode }} {{ record.materialName }}</div>
              <div class="sub-text">{{ record.spec }}</div>
            </template>
            <div class="row-tags">
              <a-tag v-if="record.batchEnabled === 1" color="blue" :bordered="false">批次</a-tag>
              <a-tag v-if="record.serialEnabled === 1" color="purple" :bordered="false">序列号</a-tag>
            </div>
          </template>
          <template v-else-if="column.key === 'location'">
            <LocationSelect v-if="kind === 'in'" v-model:value="record.locationId" :warehouse-id="form.warehouseId" />
            <span v-else>{{ locationText(record.locationCode) }}</span>
          </template>
          <template v-else-if="column.key === 'toLocation'">
            <LocationSelect v-model:value="record.toLocationId" :warehouse-id="form.toWarehouseId" />
          </template>
          <template v-else-if="column.key === 'batchNo'">
            <a-form-item
              v-if="kind === 'in'"
              :name="['items', index, 'batchNo']"
              :rules="record.batchEnabled === 1 ? requiredRule('请填写批次号') : []"
              class="cell-item"
            >
              <a-input v-model:value="record.batchNo" :maxlength="64" :disabled="record.batchEnabled !== 1" placeholder="批次号" />
            </a-form-item>
            <span v-else>{{ record.batchNo || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'productionDate'">
            <a-date-picker v-model:value="record.productionDate" value-format="YYYY-MM-DD" :disabled="record.batchEnabled !== 1" class="full" />
          </template>
          <template v-else-if="column.key === 'expiryDate'">
            <a-date-picker v-model:value="record.expiryDate" value-format="YYYY-MM-DD" :disabled="record.batchEnabled !== 1" class="full" />
          </template>
          <template v-else-if="column.key === 'qty'">
            <a-form-item :name="['items', index, 'qty']" :rules="requiredRule('请填写数量')" class="cell-item">
              <a-input-number
                v-model:value="record.qty"
                :min="0"
                :precision="record.serialEnabled === 1 ? 0 : 4"
                :addon-after="record.unit || undefined"
                class="full"
              />
            </a-form-item>
            <div v-if="record.available != null" class="sub-text">可用 {{ formatQty(record.available) }}</div>
          </template>
          <template v-else-if="column.key === 'serialNos'">
            <SerialInput
              v-if="record.serialEnabled === 1"
              v-model:value="record.serialNos"
              :expected="record.qty ?? 0"
              :source="kind === 'in' ? undefined : serialSource(record)"
              :placeholder="kind === 'in' ? '扫码或输入序列号' : '选择或扫码'"
            />
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'remark'">
            <a-input v-model:value="record.remark" :maxlength="255" />
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" danger size="small" @click="form.items.splice(index, 1)">
              <template #icon><DeleteOutlined /></template>
            </a-button>
          </template>
        </template>
      </a-table>
    </a-form>

    <StockPicker v-model:open="pickerOpen" :warehouse-id="form.warehouseId" @pick="onPick" />
  </FormDrawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import LocationSelect from './LocationSelect.vue'
import MaterialSelect from './MaterialSelect.vue'
import SerialInput from './SerialInput.vue'
import StockPicker from './StockPicker.vue'
import WarehouseSelect from './WarehouseSelect.vue'
import { addDoc, getDoc, updateDoc } from '@/api/wms/doc'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { toOptions } from '@/utils/dict'
import { DOC_KINDS, availableQty, fillPutaway, formatQty, locationText, rowKey, today } from './wms'

/**
 * 新增 / 编辑出入库单（草稿）。入库手工录入物料；出库、调拨从仓库现有库存中选择。
 * 通过 ref 调用 openCreate() / openEdit(record)，保存成功后触发 success
 */
const props = defineProps({
  kind: { type: String, required: true }
})
const emit = defineEmits(['success'])

const conf = computed(() => DOC_KINDS[props.kind])
const dict = useDict('wms_in_type', 'wms_out_type')
/** 手工单据只能是其他出入库、报废出库；采购、销售、生产、项目等由业务单据生成 */
const MANUAL_TYPES = { in: ['OTHER', 'CONSIGN_IN'], out: ['OTHER', 'SCRAP'] }
/** 寄售入库：供应商的货寄售入库到供应商寄售仓（不计存货成本），只能选供应商寄售仓 */
const consignIn = computed(() => props.kind === 'in' && form.value.bizType === 'CONSIGN_IN')
const manualTypeOptions = computed(() =>
  toOptions(dict[conf.value.typeDict]).filter((o) => (MANUAL_TYPES[props.kind] || []).includes(o.value))
)

const COLUMNS = {
  in: [
    { title: '物料', key: 'material', width: 260 },
    { title: '库位', key: 'location', width: 150 },
    { title: '批次号', key: 'batchNo', width: 150 },
    { title: '生产日期', key: 'productionDate', width: 140 },
    { title: '有效期至', key: 'expiryDate', width: 140 },
    { title: '数量', key: 'qty', width: 150 },
    { title: '序列号', key: 'serialNos', width: 260 },
    { title: '备注', key: 'remark', width: 150 }
  ],
  out: [
    { title: '物料', key: 'material', width: 240 },
    { title: '库位', key: 'location', width: 110 },
    { title: '批次', key: 'batchNo', width: 130 },
    { title: '数量', key: 'qty', width: 160 },
    { title: '序列号', key: 'serialNos', width: 300 },
    { title: '备注', key: 'remark', width: 160 }
  ],
  transfer: [
    { title: '物料', key: 'material', width: 240 },
    { title: '调出库位', key: 'location', width: 110 },
    { title: '批次', key: 'batchNo', width: 130 },
    { title: '调入库位', key: 'toLocation', width: 160 },
    { title: '数量', key: 'qty', width: 160 },
    { title: '序列号', key: 'serialNos', width: 280 },
    { title: '备注', key: 'remark', width: 150 }
  ]
}
const columns = computed(() => [...COLUMNS[props.kind], { title: '', key: 'action', width: 50, fixed: 'right' }])
const scrollX = computed(() => columns.value.reduce((sum, c) => sum + c.width, 0))

const requiredRule = (msg) => [{ required: true, message: msg }]
const rules = computed(() => ({
  bizType: requiredRule(`请选择${conf.value.typeLabel}`),
  warehouseId: requiredRule('请选择仓库'),
  toWarehouseId: requiredRule('请选择调入仓库'),
  bizDate: requiredRule('请选择业务日期')
}))

const emptyForm = () => ({
  id: undefined,
  bizType: undefined,
  warehouseId: undefined,
  toWarehouseId: undefined,
  bizDate: today(),
  inTransit: false,
  remark: '',
  items: []
})

const toRow = (item) => ({
  ...item,
  key: item.id ? `item-${item.id}` : rowKey(),
  materialLabel: item.materialCode ? `${item.materialCode} ${item.materialName}` : undefined,
  locationId: item.locationId || null,
  toLocationId: item.toLocationId || null,
  batchNo: item.batchNo || undefined
})

const pickItem = (i) => ({
  materialId: i.materialId,
  locationId: i.locationId || null,
  toLocationId: i.toLocationId || null,
  batchNo: i.batchNo || null,
  productionDate: i.productionDate || null,
  expiryDate: i.expiryDate || null,
  qty: i.qty,
  serialNos: i.serialNos || null,
  remark: i.remark || null
})

const drawer = useFormDrawer({
  emptyForm,
  fetch: async (record) => {
    const doc = await getDoc(props.kind, record.id)
    return { ...doc, inTransit: doc.inTransit === true || doc.inTransit === 1, items: doc.items.map(toRow) }
  },
  create: (data) => addDoc(props.kind, data),
  update: (id, data) => updateDoc(props.kind, id, data),
  toPayload: (f) => ({
    bizType: f.bizType,
    warehouseId: f.warehouseId,
    toWarehouseId: f.toWarehouseId,
    bizDate: f.bizDate,
    ...(props.kind === 'transfer' ? { inTransit: !!f.inTransit } : {}),
    remark: f.remark,
    items: f.items.map(pickItem)
  }),
  onSuccess: () => emit('success')
})
const { form, formRef } = drawer

const totalQty = computed(() => form.value.items.reduce((sum, i) => sum + (Number(i.qty) || 0), 0))

function addRow() {
  form.value.items.push({ key: rowKey(), materialId: undefined, locationId: null, qty: undefined })
}

/** 入库选择物料后，按物料的管理方式清理批次、序列号 */
function applyMaterial(row, material) {
  Object.assign(row, {
    materialLabel: material.label,
    materialCode: material.code,
    materialName: material.name,
    spec: material.spec,
    unit: material.unit,
    batchEnabled: material.batchEnabled,
    serialEnabled: material.serialEnabled,
    defaultWarehouseId: material.defaultWarehouseId,
    defaultLocationId: material.defaultLocationId
  })
  if (material.batchEnabled !== 1) Object.assign(row, { batchNo: undefined, productionDate: null, expiryDate: null })
  if (material.serialEnabled !== 1) row.serialNos = undefined
  applyDefaultLocation(row)
  // 扫的是辅助单位条码（如整箱）：数量为空时按一个辅助单位折算成基本单位
  if (material.scanRatio && row.qty == null && material.serialEnabled !== 1) row.qty = material.scanRatio
}

/** 物料的默认仓库就是单据仓库时，带出默认库位（不覆盖已选的库位） */
function applyDefaultLocation(row) {
  if (!row.locationId && row.defaultLocationId && row.defaultWarehouseId && row.defaultWarehouseId === form.value.warehouseId) {
    row.locationId = row.defaultLocationId
  }
}

/** 按上架策略为没有选库位的行推荐库位（调拨为调入库位） */
async function putaway() {
  const transfer = props.kind === 'transfer'
  const n = await fillPutaway(transfer ? form.value.toWarehouseId : form.value.warehouseId, form.value.items, transfer ? 'toLocationId' : 'locationId')
  message.info(n ? `已为 ${n} 行推荐库位` : '没有可推荐的库位（已选库位的行不变）')
}

/** 切换寄售入库与其他入库时仓库类型不同，清空已选仓库 */
function onBizTypeChange(type) {
  const prev = form.value.bizType
  form.value.bizType = type
  if (props.kind === 'in' && form.value.warehouseId && (type === 'CONSIGN_IN') !== (prev === 'CONSIGN_IN')) onWarehouseChange(null)
}

/** 出库、调拨的明细来自所选仓库的库存，换仓库后清空明细；入库清空库位 */
function onWarehouseChange(id) {
  if (id === form.value.warehouseId) return
  form.value.warehouseId = id
  if (props.kind === 'in') {
    form.value.items.forEach((i) => {
      i.locationId = null
      applyDefaultLocation(i)
    })
  } else if (form.value.items.length) {
    form.value.items = []
    message.info('仓库已变更，已清空明细，请重新选择库存')
  }
}

function onToWarehouseChange(id) {
  form.value.toWarehouseId = id
  form.value.items.forEach((i) => (i.toLocationId = null))
}

const serialSource = (row) => ({
  warehouseId: form.value.warehouseId,
  locationId: row.locationId || undefined,
  materialId: row.materialId,
  batchNo: row.batchNo || undefined
})

// 从库存选择：同一库存位置已在明细中时跳过
const pickerOpen = ref(false)
function onPick(stocks) {
  const exists = new Set(form.value.items.map((i) => `${i.locationId || 0}|${i.materialId}|${i.batchNo || ''}`))
  let skipped = 0
  for (const s of stocks) {
    const key = `${s.locationId || 0}|${s.materialId}|${s.batchNo || ''}`
    if (exists.has(key)) {
      skipped++
      continue
    }
    form.value.items.push(
      toRow({
        materialId: s.materialId,
        materialCode: s.materialCode,
        materialName: s.materialName,
        spec: s.spec,
        unit: s.unit,
        batchEnabled: s.batchEnabled,
        serialEnabled: s.serialEnabled,
        locationId: s.locationId,
        locationCode: s.locationCode,
        batchNo: s.batchNo,
        available: availableQty(s),
        qty: undefined
      })
    )
  }
  if (skipped) message.info(`${skipped} 项已在明细中，已跳过`)
}

defineExpose({ openCreate: drawer.openCreate, openEdit: drawer.openEdit })
</script>

<style scoped>
.full {
  width: 100%;
}
.items-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 8px 0 12px;
}
.section-title {
  font-weight: 500;
}
.items-table :deep(.ant-table-cell) {
  vertical-align: top;
}
.cell-item {
  margin-bottom: 0;
}
.row-tags {
  margin-top: 4px;
}
.row-tags:empty {
  display: none;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
