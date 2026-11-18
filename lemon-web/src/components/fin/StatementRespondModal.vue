<template>
  <a-modal v-model:open="visible" :title="form.agree ? $t('fin.statementRespond.confirmTitle') : $t('fin.statementRespond.disputeTitle')" :confirm-loading="saving" @ok="submit">
    <a-form layout="vertical">
      <a-alert v-if="form.agree && tip" type="info" show-icon class="block" :message="tip" />
      <a-row v-if="form.agree && invoice" :gutter="12">
        <a-col :span="12"><a-form-item :label="$t('fin.statementRespond.invoiceNo')"><a-input v-model:value="form.invoiceNo" :maxlength="64" /></a-form-item></a-col>
        <a-col :span="12">
          <a-form-item :label="$t('fin.statementRespond.invoiceAmount')"><a-input-number v-model:value="form.invoiceAmount" :min="0" :precision="2" prefix="¥" class="full" /></a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('fin.statementRespond.invoiceDate')"><a-date-picker v-model:value="form.invoiceDate" value-format="YYYY-MM-DD" class="full" /></a-form-item>
        </a-col>
      </a-row>
      <a-form-item :label="form.agree ? $t('common.field.description') : $t('fin.statementRespond.disputeRemark')" :required="!form.agree">
        <a-textarea v-model:value="form.remark" :rows="3" :maxlength="500" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { today } from '@/components/wms/wms'

/**
 * 对账单确认 / 提出异议（供应商门户、经销商门户、财务登记客户签回结果共用）：有异议时须说明；
 * invoice 为 true 时确认可同时填写发票（供应商门户）。open(agree, invoiceAmount) 打开，提交成功后触发 done
 */
const props = defineProps({
  /** 提交：(data) => Promise，data 为 { agree, remark }（invoice 时另有 invoiceNo、invoiceAmount、invoiceDate） */
  submitter: { type: Function, required: true },
  /** 确认时显示发票信息 */
  invoice: { type: Boolean, default: false },
  /** 确认时的提示 */
  tip: { type: String, default: '' }
})
const emit = defineEmits(['done'])
const { t } = useI18n()

const visible = ref(false)
const saving = ref(false)
const form = ref({})

function open(agree, invoiceAmount) {
  form.value = { agree, remark: '', invoiceNo: '', invoiceAmount: agree ? invoiceAmount : undefined, invoiceDate: today() }
  visible.value = true
}

async function submit() {
  if (!form.value.agree && !form.value.remark?.trim()) return message.warning(t('fin.statementRespond.msgDispute'))
  const data = { agree: form.value.agree, remark: form.value.remark }
  if (props.invoice) {
    const withInvoice = form.value.agree && form.value.invoiceNo
    Object.assign(data, withInvoice
      ? { invoiceNo: form.value.invoiceNo, invoiceAmount: form.value.invoiceAmount, invoiceDate: form.value.invoiceDate }
      : { invoiceNo: null, invoiceAmount: null, invoiceDate: null })
  }
  saving.value = true
  try {
    await props.submitter(data)
    message.success(data.agree ? t('fin.statementRespond.confirmed') : t('fin.statementRespond.disputed'))
    visible.value = false
    emit('done', data.agree)
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
