<template>
  <a-modal :open="visible" :title="$t('wms.stockLimit.batchTitle', { n: ids.length })" :confirm-loading="saving" width="min(520px, 100vw)" @ok="save" @cancel="visible = false">
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-form-item :label="$t('wms.stockLimit.alertMode')" name="alertMode">
        <a-radio-group v-model:value="form.alertMode" button-style="solid">
          <a-radio-button v-for="o in toOptions(dict.wms_stock_alert_mode)" :key="o.value" :value="o.value">{{ o.label }}</a-radio-button>
        </a-radio-group>
      </a-form-item>
      <a-alert type="info" show-icon class="tip"
        :message="form.alertMode === StockAlertMode.WAREHOUSE ? $t('wms.stockLimit.batchWarehouseTip') : $t('wms.stockLimit.batchTotalTip')" />
      <a-form-item v-if="form.alertMode === StockAlertMode.WAREHOUSE" :label="$t('wms.stockLimit.warehouse')" name="warehouseId"
        :rules="[{ required: true, message: $t('wms.stockLimit.selectWarehouse') }]">
        <WarehouseSelect v-model:value="form.warehouseId" />
      </a-form-item>
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12">
          <a-form-item :label="form.alertMode === StockAlertMode.WAREHOUSE ? $t('wms.stockLimit.minQty') : $t('wms.stock.safetyStock')" name="minQty">
            <a-input-number v-model:value="form.minQty" :min="0" :precision="4" class="full" :placeholder="$t('wms.material.noAlert')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('wms.stockLimit.maxStock')" name="maxQty" :rules="maxRules">
            <a-input-number v-model:value="form.maxQty" :min="0" :precision="4" class="full" :placeholder="$t('wms.material.noAlert')" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { batchSetStockLimits } from '@/api/wms/material'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'
import { StockAlertMode } from '@/constants/enums'
import { t } from '@/locales'

/** 批量设置库存预警（物料列表选中的物料）：按总库存，或某个仓库的上下限。用法：ref.open(物料 ID 列表)，保存后触发 saved */
const emit = defineEmits(['saved'])
const dict = useDict('wms_stock_alert_mode')
const visible = ref(false)
const saving = ref(false)
const ids = ref([])
const formRef = ref()
const form = reactive({ alertMode: StockAlertMode.TOTAL, warehouseId: undefined, minQty: null, maxQty: null })
const maxRules = [
  {
    validator: () =>
      form.minQty != null && form.maxQty != null && Number(form.minQty) > Number(form.maxQty) ? Promise.reject(t('wms.stockLimit.minGreaterMax')) : Promise.resolve()
  }
]

function open(materialIds) {
  ids.value = [...materialIds]
  Object.assign(form, { alertMode: StockAlertMode.TOTAL, warehouseId: undefined, minQty: null, maxQty: null })
  formRef.value?.clearValidate()
  visible.value = true
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const n = await batchSetStockLimits({
      materialIds: ids.value,
      alertMode: form.alertMode,
      warehouseId: form.alertMode === StockAlertMode.WAREHOUSE ? form.warehouseId : null,
      minQty: form.minQty ?? null,
      maxQty: form.maxQty ?? null
    })
    message.success(t('wms.stockLimit.batchSuccess', { n }))
    visible.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.tip {
  margin-bottom: 16px;
}
.full {
  width: 100%;
}
</style>
