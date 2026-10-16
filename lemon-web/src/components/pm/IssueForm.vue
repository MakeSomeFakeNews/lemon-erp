<template>
  <SubPage :open="open" :title="project ? `项目领料 - ${project.code} ${project.name}` : '项目领料'" width="min(1100px, 100vw)"
            destroy-on-close @close="emit('update:open', false)">
    <a-alert type="info" show-icon class="tip"
             message="从仓库现有库存中选择库位、批次领用，保存后生成「项目领料」出库单并扣减库存，按物料单位成本（最近一期成本核算单价，没有时取采购均价）计入项目材料成本。" />
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="8">
          <a-form-item label="仓库" name="warehouseId" :rules="[{ required: true, message: '请选择仓库' }]">
            <WarehouseSelect :value="form.warehouseId" :allow-clear="false" @update:value="onWarehouseChange" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="6">
          <a-form-item label="业务日期" name="bizDate" :rules="[{ required: true, message: '请选择业务日期' }]">
            <a-date-picker v-model:value="form.bizDate" value-format="YYYY-MM-DD" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="10">
          <a-form-item label="备注" name="remark">
            <a-input v-model:value="form.remark" :maxlength="255" placeholder="选填" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="items-head">
        <span class="section-title">领料明细</span>
        <a-button type="primary" ghost size="small" @click="addRow"><template #icon><PlusOutlined /></template>添加物料</a-button>
      </div>
      <a-table row-key="key" size="small" class="items-table" :columns="columns" :data-source="form.items" :pagination="false"
               :scroll="{ x: 1000 }">
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'material'">
            <a-form-item :name="['items', index, 'materialId']" :rules="[{ required: true, message: '请选择物料' }]" class="cell-item">
              <MaterialSelect v-model:value="record.materialId" :label="record.materialLabel" @select="(m) => onMaterial(record, m)" />
            </a-form-item>
            <div class="row-tags">
              <a-tag v-if="record.batchEnabled === 1" color="blue" :bordered="false">批次</a-tag>
              <a-tag v-if="record.serialEnabled === 1" color="purple" :bordered="false">序列号</a-tag>
            </div>
          </template>
          <template v-else-if="column.key === 'slot'">
            <a-form-item :name="['items', index, 'slot']" :rules="[{ required: true, message: '请选择库位 / 批次' }]" class="cell-item">
              <StockSlotSelect
                v-model:value="record.slot"
                :label="record.slotLabel"
                :warehouse-id="form.warehouseId"
                :material-id="record.materialId"
                :disabled="!record.materialId"
                @select="(s) => Object.assign(record, { locationId: s.locationId, batchNo: s.batchNo, slotLabel: s.label, available: Number(s.qty), serialNos: undefined })"
              />
            </a-form-item>
            <div v-if="record.available != null" class="sub-text">可用 {{ formatQty(record.available) }}</div>
          </template>
          <template v-else-if="column.key === 'qty'">
            <a-form-item :name="['items', index, 'qty']" :rules="qtyRules(record)" class="cell-item">
              <a-input-number v-model:value="record.qty" :min="0" :precision="record.serialEnabled === 1 ? 0 : 4"
                              :addon-after="record.unit || undefined" class="full" />
            </a-form-item>
          </template>
          <template v-else-if="column.key === 'serialNos'">
            <SerialInput
              v-if="record.serialEnabled === 1"
              v-model:value="record.serialNos"
              :expected="record.qty ?? 0"
              :source="record.slot ? { warehouseId: form.warehouseId, locationId: record.locationId || undefined, materialId: record.materialId, batchNo: record.batchNo || undefined } : undefined"
            />
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" danger size="small" @click="form.items.splice(index, 1)"><DeleteOutlined /></a-button>
          </template>
        </template>
      </a-table>
    </a-form>
    <template #footer>
      <div class="drawer-footer">
        <a-button @click="emit('update:open', false)">取消</a-button>
        <a-button type="primary" :loading="saving" :disabled="!form.items.length" @click="submit">确认领料</a-button>
      </div>
    </template>
  </SubPage>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import StockSlotSelect from '@/components/scm/StockSlotSelect.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import SerialInput from '@/components/wms/SerialInput.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { issueMaterial } from '@/api/pm'
import { formatQty, rowKey, today } from '@/components/wms/wms'

/** 项目领料：生成「项目领料」出库单并直接确认，计入项目材料成本 */
const props = defineProps({
  open: Boolean,
  project: Object
})
const emit = defineEmits(['update:open', 'success'])

const columns = [
  { title: '物料', key: 'material', width: 260 },
  { title: '库位 / 批次', key: 'slot', width: 260 },
  { title: '数量', key: 'qty', width: 150 },
  { title: '序列号', key: 'serialNos', width: 240 },
  { title: '', key: 'action', width: 50, fixed: 'right' }
]

const formRef = ref()
const form = ref({ warehouseId: undefined, bizDate: today(), remark: '', items: [] })
const saving = ref(false)

const emptyRow = () => ({
  key: rowKey(), materialId: undefined, materialLabel: undefined, unit: undefined, batchEnabled: 0, serialEnabled: 0,
  slot: undefined, slotLabel: undefined, locationId: null, batchNo: undefined, available: undefined, qty: undefined, serialNos: undefined
})

watch(
  () => props.open,
  (open) => {
    if (open) form.value = { warehouseId: undefined, bizDate: today(), remark: '', items: [emptyRow()] }
  }
)

const addRow = () => form.value.items.push(emptyRow())

function onMaterial(record, m) {
  Object.assign(record, {
    materialLabel: m.label, unit: m.unit, batchEnabled: m.batchEnabled, serialEnabled: m.serialEnabled,
    slot: undefined, slotLabel: undefined, locationId: null, batchNo: undefined, available: undefined, serialNos: undefined
  })
}

function onWarehouseChange(id) {
  form.value.warehouseId = id
  form.value.items.forEach((i) => Object.assign(i, { slot: undefined, slotLabel: undefined, locationId: null, batchNo: undefined,
    available: undefined, serialNos: undefined }))
}

const qtyRules = (record) => [
  { required: true, message: '请填写数量' },
  {
    validator: (_, v) => {
      if (v != null && v <= 0) return Promise.reject(new Error('数量必须大于 0'))
      if (v != null && record.available != null && v > record.available) return Promise.reject(new Error('超过可用库存'))
      return Promise.resolve()
    }
  }
]

async function submit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    await issueMaterial(props.project.id, {
      warehouseId: form.value.warehouseId,
      bizDate: form.value.bizDate,
      remark: form.value.remark || null,
      items: form.value.items.map((i) => ({
        materialId: i.materialId,
        locationId: i.locationId || null,
        batchNo: i.batchNo || null,
        qty: i.qty,
        serialNos: i.serialNos || null
      }))
    })
    message.success('领料成功，已计入项目材料成本')
    emit('update:open', false)
    emit('success')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.tip {
  margin-bottom: 16px;
}
.full {
  width: 100%;
}
.items-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.section-title {
  font-weight: 600;
}
.items-table :deep(.ant-table-cell) {
  vertical-align: top;
}
.cell-item {
  margin-bottom: 0;
}
.row-tags:empty {
  display: none;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
