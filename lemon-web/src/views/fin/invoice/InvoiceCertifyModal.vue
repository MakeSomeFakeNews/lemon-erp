<template>
  <a-modal v-model:open="visible" :title="$t('fin.invoice.certifyTitle')" :confirm-loading="certifying" :ok-text="$t('fin.invoice.certify')" @ok="certify(certifyPeriod)">
    <p>{{ $t('fin.invoice.certifyTip', { n: certifiable.length }) }}</p>
    <a-space>
      <span>{{ $t('fin.invoice.certifyPeriod') }}</span>
      <PeriodPicker v-model:value="certifyPeriod" />
      <a-popconfirm :title="$t('fin.invoice.confirmUncertify')" @confirm="certify(null)">
        <a-button :loading="certifying">{{ $t('fin.invoice.uncertify') }}</a-button>
      </a-popconfirm>
    </a-space>
  </a-modal>
</template>

<script setup>
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import PeriodPicker from '@/components/fin/PeriodPicker.vue'
import { certifyInvoice } from '@/api/fin'
import { currentPeriod, periodText } from '@/components/fin/fin'

/** 进项认证 / 取消认证：certifiable 为勾选的有效发票 ID，通过 ref 调用 open()，成功后触发 success */
const props = defineProps({ certifiable: { type: Array, default: () => [] } })
const emit = defineEmits(['success'])
const { t } = useI18n()

const visible = ref(false)
const certifying = ref(false)
const certifyPeriod = ref(currentPeriod())
function open() {
  certifyPeriod.value = currentPeriod()
  visible.value = true
}
async function certify(period) {
  certifying.value = true
  try {
    await certifyInvoice(props.certifiable, period)
    message.success(period ? t('fin.invoice.certified', { period: periodText(period) }) : t('fin.invoice.uncertified2'))
    visible.value = false
    emit('success')
  } finally {
    certifying.value = false
  }
}

defineExpose({ open })
</script>
