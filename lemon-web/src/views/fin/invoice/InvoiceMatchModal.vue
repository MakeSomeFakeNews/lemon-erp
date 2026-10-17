<template>
  <a-modal
    v-model:open="visible"
    :title="matchInvoiceRow ? $t('fin.invoice.matchTitle', { no: matchInvoiceRow.invoiceNo }) : ''"
    width="min(900px, 100vw)"
    :confirm-loading="matching"
    :ok-text="$t('fin.invoice.match')"
    @ok="submitMatch"
  >
    <template v-if="matchInvoiceRow">
      <p>
        {{ $t('fin.invoice.matchSummary', { partner: matchInvoiceRow.partnerName, amount: formatMoney(matchInvoiceRow.amount) }) }}
        <b>¥{{ formatMoney(unmatchedAmount(matchInvoiceRow)) }}</b>{{ $t('fin.invoice.matchSummary2') }} <b class="total">¥{{ formatMoney(itemTotal) }}</b>
      </p>
      <a-table
        row-key="id"
        size="small"
        :columns="billColumns"
        :data-source="bills"
        :loading="billLoading"
        :pagination="false"
        :row-selection="{ selectedRowKeys: checkedBills, onChange: onCheckBills }"
        :scroll="{ x: 860, y: 360 }"
        :locale="{ emptyText: conf.noBills }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'amount'">{{ formatMoney(record.amount) }}</template>
          <template v-else-if="column.key === 'invoiced'">{{ formatMoney(record.invoicedAmount ?? 0) }}</template>
          <template v-else-if="column.key === 'remaining'">{{ formatMoney(billRemaining(record)) }}</template>
          <template v-else-if="column.key === 'apply'">
            <a-input-number
              v-model:value="record.apply"
              :precision="2"
              :min="Math.min(0, billRemaining(record))"
              :max="Math.max(0, billRemaining(record))"
              :disabled="!checkedBills.includes(record.id)"
              class="full"
            />
          </template>
        </template>
      </a-table>
    </template>
  </a-modal>
</template>

<script setup>
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { invoiceOpenBills, matchInvoice } from '@/api/fin'
import { formatMoney } from '@/components/fin/fin'
import { KINDS, billColumns as makeBillColumns, billRemaining, unmatchedAmount, useInvoiceBills } from './invoice'

/** 勾稽导入的发票：通过 ref 调用 open(发票)，成功后触发 success */
const props = defineProps({ kind: { type: String, required: true } })
const emit = defineEmits(['success'])
const { t } = useI18n()
const conf = computed(() => KINDS[props.kind])

const billColumns = makeBillColumns()
const visible = ref(false)
const matching = ref(false)
const matchInvoiceRow = ref()
const { bills, billLoading, checkedBills, itemTotal, resetBills, fetchBills, onCheckBills, billItems } = useInvoiceBills()

async function open(record) {
  matchInvoiceRow.value = record
  resetBills()
  visible.value = true
  const left = unmatchedAmount(record)
  await fetchBills(() => invoiceOpenBills(record.kind, record.partnerId))
  // 金额相等的单据默认勾选
  const exact = bills.value.find((b) => billRemaining(b) === left)
  if (exact) checkedBills.value = [exact.id]
}
async function submitMatch() {
  const items = billItems()
  if (!items.length) {
    message.warning(t('fin.invoice.selectBills'))
    return
  }
  matching.value = true
  try {
    await matchInvoice(matchInvoiceRow.value.id, items)
    message.success(t('fin.invoice.matched'))
    visible.value = false
    emit('success')
  } finally {
    matching.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.total {
  font-size: 16px;
  color: var(--ant-color-primary, #1677ff);
}
.full {
  width: 100%;
}
</style>
