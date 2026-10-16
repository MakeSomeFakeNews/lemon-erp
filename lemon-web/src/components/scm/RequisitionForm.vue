<template>
  <FormDrawer page :drawer="drawer" create-title="新增请购单" edit-title="编辑请购单" width="min(1100px, 100vw)">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item label="请购日期" name="reqDate">
            <a-date-picker v-model:value="form.reqDate" value-format="YYYY-MM-DD" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="18">
          <a-form-item label="请购说明" name="remark">
            <a-input v-model:value="form.remark" :maxlength="255" placeholder="选填，如用途、项目" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="items-head">
        <span class="section-title">明细（{{ form.items.length }} 行）</span>
        <a-button type="primary" ghost @click="addRow">
          <template #icon><PlusOutlined /></template>添加物料
        </a-button>
      </div>

      <a-table
        row-key="key"
        size="small"
        class="items-table"
        :columns="columns"
        :data-source="form.items"
        :pagination="false"
        :scroll="{ x: 1000 }"
        :locale="{ emptyText: '点击「添加物料」录入明细' }"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'material'">
            <a-form-item :name="['items', index, 'materialId']" :rules="requiredRule('请选择物料')" class="cell-item">
              <MaterialSelect v-model:value="record.materialId" :label="record.materialLabel" @select="(m) => applyMaterial(record, m)" />
            </a-form-item>
          </template>
          <template v-else-if="column.key === 'qty'">
            <a-form-item :name="['items', index, 'qty']" :rules="requiredRule('请填写数量')" class="cell-item">
              <a-input-number v-model:value="record.qty" :min="0" :precision="4" :addon-after="record.unit || undefined" class="full" />
            </a-form-item>
          </template>
          <template v-else-if="column.key === 'requiredDate'">
            <a-date-picker v-model:value="record.requiredDate" value-format="YYYY-MM-DD" placeholder="需求日期" class="full" />
          </template>
          <template v-else-if="column.key === 'supplier'">
            <PartnerSelect
              v-model:value="record.supplierId"
              type="supplier"
              :label="record.supplierName"
              placeholder="选填，默认供应商"
              allow-clear
            />
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
  </FormDrawer>
</template>

<script setup>
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import PartnerSelect from './PartnerSelect.vue'
import { addRequisition, getRequisition, updateRequisition } from '@/api/scm/sourcing'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { rowKey, today } from '@/components/wms/wms'

/**
 * 新增 / 编辑请购单（草稿），明细可指定建议供应商，不指定时转采购订单按物料的默认供应商
 * 通过 ref 调用 openCreate() / openEdit(record)，保存成功后触发 success(id)
 */
const emit = defineEmits(['success'])

const columns = [
  { title: '物料', key: 'material', width: 280 },
  { title: '数量', key: 'qty', width: 150 },
  { title: '需求日期', key: 'requiredDate', width: 140 },
  { title: '建议供应商', key: 'supplier', width: 220 },
  { title: '备注', key: 'remark', width: 150 },
  { title: '', key: 'action', width: 50, fixed: 'right' }
]

const requiredRule = (msg) => [{ required: true, message: msg }]
const rules = { reqDate: requiredRule('请选择请购日期') }

const emptyForm = () => ({ id: undefined, reqDate: today(), remark: '', items: [] })

const toRow = (item) => ({
  ...item,
  key: item.id ? `item-${item.id}` : rowKey(),
  materialLabel: item.materialCode ? `${item.materialCode} ${item.materialName}` : undefined,
  qty: item.qty != null ? Number(item.qty) : undefined
})

const drawer = useFormDrawer({
  emptyForm,
  fetch: async (record) => {
    const req = await getRequisition(record.id)
    return { ...req, items: req.items.map(toRow) }
  },
  create: async (data) => emit('success', await addRequisition(data)),
  update: async (id, data) => {
    await updateRequisition(id, data)
    emit('success', id)
  },
  toPayload: (f) => ({
    reqDate: f.reqDate,
    remark: f.remark || null,
    items: f.items.map((i) => ({
      materialId: i.materialId,
      qty: i.qty,
      requiredDate: i.requiredDate || null,
      supplierId: i.supplierId || null,
      remark: i.remark || null
    }))
  })
})
const { form, formRef } = drawer

function addRow() {
  form.value.items.push({ key: rowKey(), materialId: undefined, qty: undefined, requiredDate: undefined, supplierId: undefined })
}

function applyMaterial(row, material) {
  Object.assign(row, { materialLabel: material.label, unit: material.unit })
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
</style>
