<template>
  <a-modal :open="open" :title="`红字冲销 ${voucher?.voucherNo || ''}`" :confirm-loading="saving" @ok="submit" @cancel="emit('update:open', false)">
    <a-alert
      type="info"
      show-icon
      class="tip"
      message="生成一张科目、方向相同而金额为负数的红字凭证（未审核），原凭证标记为已冲销；删除红字凭证后原凭证可以重新冲销。"
    />
    <a-form layout="vertical">
      <a-form-item label="冲销日期" extra="红字凭证记入该日期所在的期间，期间须未结账">
        <a-date-picker v-model:value="date" value-format="YYYY-MM-DD" :allow-clear="false" class="full" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { reverseVoucher } from '@/api/fin'
import { today } from '@/components/wms/wms'

/** 红字冲销凭证：成功后触发 success(红字凭证 ID) */
const props = defineProps({
  open: Boolean,
  voucher: Object
})
const emit = defineEmits(['update:open', 'success'])

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
    message.success('已生成红字凭证（未审核）')
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
