<template>
  <a-modal :open="open" :title="$t('fin.reverseModal.title', { no: voucher?.voucherNo || '' })" :confirm-loading="saving" @ok="submit" @cancel="emit('update:open', false)">
    <a-alert
      type="info"
      show-icon
      class="tip"
      :message="$t('fin.reverseModal.tip')"
    />
    <a-form layout="vertical">
      <a-form-item :label="$t('fin.reverseModal.date')" :extra="$t('fin.reverseModal.dateExtra')">
        <a-date-picker v-model:value="date" value-format="YYYY-MM-DD" :allow-clear="false" class="full" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { reverseVoucher } from '@/api/fin'
import { today } from '@/components/wms/wms'

/** 红字冲销凭证：成功后触发 success(红字凭证 ID) */
const props = defineProps({
  open: Boolean,
  voucher: Object
})
const emit = defineEmits(['update:open', 'success'])

const { t } = useI18n()
const date = ref(today())
const saving = ref(false)
watch(
  () => props.open,
  (v) => v && (date.value = today())
)

async function submit() {
  saving.value = true
  try {
    const id = await reverseVoucher(props.voucher.id, date.value)
    message.success(t('fin.reverseModal.success'))
    emit('update:open', false)
    emit('success', id)
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
</style>
