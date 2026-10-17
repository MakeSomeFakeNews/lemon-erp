<template>
  <a-modal v-model:open="visible" :title="$t('mes.wo.addRework')" width="640px" :confirm-loading="saving" @ok="submit">
    <a-alert
      type="info"
      show-icon
      class="block"
      :message="$t('mes.wo.reworkTip')"
    />
    <a-form ref="reworkFormRef" :model="reworkForm" layout="vertical">
      <a-form-item v-if="reworkForm.sourceWoId" :label="$t('mes.wo.reworkSource')">{{ reworkForm.sourceWoNo }}</a-form-item>
      <a-form-item :label="$t('mes.wo.product')" name="materialId" :rules="[{ required: true, message: $t('mes.wo.productRequired') }]">
        <MaterialSelect
          v-model:value="reworkForm.materialId"
          :label="reworkForm.materialLabel"
          :disabled="!!reworkForm.sourceWoId"
          @select="(m) => (reworkForm.materialLabel = m.label)"
        />
      </a-form-item>
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('mes.wo.reworkQty')" name="qty" :rules="[{ required: true, message: $t('mes.wo.qtyRequired') }]">
            <a-input-number v-model:value="reworkForm.qty" :min="0" :precision="4" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('mes.wo.finishWarehouse')" name="warehouseId">
            <WarehouseSelect v-model:value="reworkForm.warehouseId" :placeholder="$t('mes.wo.finishWarehousePh')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('mes.wo.planStart')" name="planStart">
            <a-date-picker v-model:value="reworkForm.planStart" value-format="YYYY-MM-DD" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('mes.wo.planEnd')" name="planEnd">
            <a-date-picker v-model:value="reworkForm.planEnd" value-format="YYYY-MM-DD" class="full" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('common.field.remark')" name="remark">
            <a-textarea v-model:value="reworkForm.remark" :rows="2" :maxlength="255" :placeholder="$t('mes.wo.reworkRemarkPh')" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>
</template>

<script setup>
import { reactive, ref } from 'vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { addReworkWo } from '@/api/mes/wo'
import { today } from '@/components/wms/wms'

/** 新建返工工单：通过 ref 调用 open(来源工单)，从工单详情进入时带出产品和来源工单；成功后触发 success(新工单ID) */
const emit = defineEmits(['success'])

const visible = ref(false)
const saving = ref(false)
const reworkFormRef = ref()
const emptyRework = () => ({
  sourceWoId: undefined,
  sourceWoNo: undefined,
  materialId: undefined,
  materialLabel: undefined,
  qty: undefined,
  planStart: today(),
  planEnd: undefined,
  warehouseId: undefined,
  remark: ''
})
const reworkForm = reactive(emptyRework())
function open(wo) {
  Object.assign(reworkForm, emptyRework())
  if (wo) {
    Object.assign(reworkForm, {
      sourceWoId: wo.id,
      sourceWoNo: wo.woNo,
      materialId: wo.materialId,
      materialLabel: `${wo.materialCode} ${wo.materialName}`,
      warehouseId: wo.warehouseId || undefined
    })
  }
  visible.value = true
}
async function submit() {
  try {
    await reworkFormRef.value.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    const f = reworkForm
    const id = await addReworkWo(
      { materialId: f.materialId, qty: f.qty, planStart: f.planStart || null, planEnd: f.planEnd || null, warehouseId: f.warehouseId || null, remark: f.remark || null },
      f.sourceWoId
    )
    visible.value = false
    emit('success', id)
  } finally {
    saving.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.full {
  width: 100%;
}
.block {
  margin-bottom: 16px;
}
</style>
