<template>
  <a-modal v-model:open="visible" :title="$t('mes.wo.splitTitle', { no: detail?.woNo })" :confirm-loading="saving" @ok="submit">
    <a-alert
      type="info"
      show-icon
      class="block"
      :message="$t('mes.wo.splitTip')"
    />
    <a-form layout="vertical">
      <a-form-item :label="$t('mes.wo.splitQty')" required :extra="detail ? $t('mes.wo.splitExtra', { qty: formatQty(detail.qty), unit: detail.unit || '' }) : ''">
        <a-input-number v-model:value="splitForm.qty" :min="0" :precision="4" :addon-after="detail?.unit || undefined" class="full" />
      </a-form-item>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item :label="$t('mes.wo.planStart')">
            <a-date-picker v-model:value="splitForm.planStart" value-format="YYYY-MM-DD" class="full" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('mes.wo.planEnd')">
            <a-date-picker v-model:value="splitForm.planEnd" value-format="YYYY-MM-DD" class="full" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { splitWo } from '@/api/mes/wo'
import { formatQty } from '@/components/wms/wms'

/** 拆分工单：通过 ref 调用 open()，成功后触发 success(新工单ID) */
const props = defineProps({ detail: { type: Object, default: undefined } })
const emit = defineEmits(['success'])
const { t } = useI18n()

const visible = ref(false)
const saving = ref(false)
const splitForm = reactive({ qty: undefined, planStart: undefined, planEnd: undefined })
function open() {
  Object.assign(splitForm, { qty: undefined, planStart: props.detail.planStart, planEnd: props.detail.planEnd })
  visible.value = true
}
async function submit() {
  if (!(Number(splitForm.qty) > 0)) {
    message.warning(t('mes.wo.splitQtyRequired'))
    return
  }
  saving.value = true
  try {
    const id = await splitWo(props.detail.id, { qty: splitForm.qty, planStart: splitForm.planStart || null, planEnd: splitForm.planEnd || null })
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
