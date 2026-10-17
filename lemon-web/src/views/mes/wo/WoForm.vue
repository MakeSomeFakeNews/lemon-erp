<template>
  <FormDrawer :drawer="drawer" :create-title="$t('mes.wo.add')" :edit-title="$t('mes.wo.edit')">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-form-item :label="$t('mes.wo.product')" name="materialId">
        <MaterialSelect v-model:value="form.materialId" :label="form.materialLabel" @select="onProductSelect" />
      </a-form-item>
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12">
          <a-form-item label="BOM" name="bomId">
            <a-select v-model:value="form.bomId" :options="bomOptions" :placeholder="$t('mes.wo.defaultBom')" allow-clear />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('mes.wo.qty')" name="qty">
            <a-input-number v-model:value="form.qty" :min="0" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('mes.wo.planStart')" name="planStart">
            <a-date-picker v-model:value="form.planStart" value-format="YYYY-MM-DD" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('mes.wo.planEnd')" name="planEnd">
            <a-date-picker v-model:value="form.planEnd" value-format="YYYY-MM-DD" class="full" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('mes.wo.finishWarehouse')" name="warehouseId">
            <WarehouseSelect v-model:value="form.warehouseId" :placeholder="$t('mes.wo.finishWarehousePh')" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('common.field.remark')" name="remark">
            <a-textarea v-model:value="form.remark" :rows="2" :maxlength="255" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import FormDrawer from '@/components/FormDrawer.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { bomsOfMaterial } from '@/api/mes/base'
import { addWo, getWo, updateWo } from '@/api/mes/wo'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { today } from '@/components/wms/wms'

/** 新增 / 编辑生产工单（草稿）。通过 ref 调用 openCreate() / openEdit(record)，保存成功后触发 success */
const emit = defineEmits(['success'])
const { t } = useI18n()

const boms = ref([])
const bomOptions = computed(() => boms.value.map((b) => ({ value: b.id, label: b.isDefault === 1 ? t('mes.wo.bomDefault', { v: b.version }) : b.version })))
async function loadBoms(materialId) {
  boms.value = materialId ? await bomsOfMaterial(materialId) : []
}
async function onProductSelect(m) {
  form.value.materialLabel = m.label
  form.value.bomId = undefined
  await loadBoms(m.id)
}

const rules = {
  materialId: [{ required: true, message: t('mes.wo.productRequired') }],
  qty: [{ required: true, message: t('mes.wo.qtyRequired') }]
}
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, materialId: undefined, bomId: undefined, qty: undefined, planStart: today(), planEnd: undefined, warehouseId: undefined, remark: '' }),
  fetch: async (record) => {
    const wo = await getWo(record.id)
    await loadBoms(wo.materialId)
    return { ...wo, qty: Number(wo.qty), materialLabel: `${wo.materialCode} ${wo.materialName}` }
  },
  beforeOpen: (record) => {
    if (!record) boms.value = []
  },
  create: addWo,
  update: updateWo,
  toPayload: (f) => ({
    materialId: f.materialId,
    bomId: f.bomId || null,
    qty: f.qty,
    planStart: f.planStart || null,
    planEnd: f.planEnd || null,
    warehouseId: f.warehouseId || null,
    remark: f.remark || null
  }),
  onSuccess: () => emit('success')
})
const { formRef, form } = drawer

defineExpose({ openCreate: drawer.openCreate, openEdit: drawer.openEdit })
</script>

<style scoped>
.full {
  width: 100%;
}
</style>
