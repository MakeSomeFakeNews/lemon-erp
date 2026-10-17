<template>
  <a-modal v-model:open="visible" :title="$t('mes.wo.qtyTitle', { no: detail?.woNo })" :confirm-loading="saving" @ok="submit">
    <a-alert type="info" show-icon class="block" :message="$t('mes.wo.qtyTip')" />
    <a-form layout="vertical">
      <a-form-item :label="$t('mes.wo.qty')" required :extra="detail ? $t('mes.wo.qtyExtra', { qty: formatQty(detail.qty), finished: formatQty(detail.finishedQty), unit: detail.unit || '' }) : ''">
        <a-input-number v-model:value="qtyValue" :min="0" :precision="4" :addon-after="detail?.unit || undefined" class="full" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { changeWoQty } from '@/api/mes/wo'
import { formatQty } from '@/components/wms/wms'

/** 工单改量：通过 ref 调用 open()，成功后触发 success */
const props = defineProps({ detail: { type: Object, default: undefined } })
const emit = defineEmits(['success'])
const { t } = useI18n()

const visible = ref(false)
const saving = ref(false)
const qtyValue = ref()
function open() {
  qtyValue.value = Number(props.detail.qty)
  visible.value = true
}
async function submit() {
  if (!(Number(qtyValue.value) > 0)) {
    message.warning(t('mes.wo.qtyInputRequired'))
    return
  }
  saving.value = true
  try {
    await changeWoQty(props.detail.id, qtyValue.value)
    message.success(t('mes.wo.qtyChanged'))
    visible.value = false
    emit('success')
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
