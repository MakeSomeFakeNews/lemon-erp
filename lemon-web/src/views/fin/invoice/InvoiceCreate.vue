<template>
  <SubPage :open="visible" :title="conf.registerTitle" width="min(1000px, 100vw)" destroy-on-close @close="visible = false">
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12">
          <a-form-item :label="conf.partnerLabel" name="partnerId" :rules="[{ required: true, message: $t('common.rule.requiredSelect', { name: conf.partnerLabel }) }]">
            <PartnerSelect v-model:value="form.partnerId" :type="conf.partner" @update:value="loadBills" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('fin.invoice.invoiceNo')" name="invoiceNo" :rules="[{ required: true, whitespace: true, message: $t('common.rule.required', { name: $t('fin.invoice.invoiceNo') }) }]">
            <a-input v-model:value="form.invoiceNo" :maxlength="32" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6">
          <a-form-item :label="$t('fin.invoice.invoiceType')" name="invoiceType">
            <a-select v-model:value="form.invoiceType" :options="toOptions(dict.fin_invoice_type)" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6">
          <a-form-item :label="$t('fin.invoice.invoiceDate')" name="invoiceDate" :rules="[{ required: true, message: $t('common.rule.requiredSelect', { name: $t('fin.invoice.invoiceDate') }) }]">
            <a-date-picker v-model:value="form.invoiceDate" value-format="YYYY-MM-DD" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6">
          <a-form-item :label="$t('fin.invoice.amount')" name="amount" :rules="[{ required: true, message: $t('common.rule.required', { name: $t('fin.invoice.amount') }) }]" :extra="$t('fin.invoice.amountExtra')">
            <a-input-number v-model:value="form.amount" :precision="2" prefix="¥" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6">
          <a-form-item :label="$t('fin.invoice.taxAmount')" name="taxAmount" :rules="[{ required: true, message: $t('common.rule.required', { name: $t('fin.invoice.taxAmount') }) }]" :extra="$t('fin.invoice.taxExtra')">
            <a-input-number v-model:value="form.taxAmount" :precision="2" prefix="¥" class="full" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('common.field.remark')" name="remark">
            <a-input v-model:value="form.remark" :maxlength="255" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
    <div class="bills-head">
      <span class="section-title">{{ conf.billsTitle }}</span>
      <a-space>
        <span>{{ $t('fin.invoice.matchTotal') }} <b class="total">¥{{ formatMoney(itemTotal) }}</b></span>
        <a-tag v-if="form.amount != null && Math.abs(itemTotal - Number(form.amount)) >= 0.005" color="red" :bordered="false">{{ $t('fin.invoice.mismatch') }}</a-tag>
      </a-space>
    </div>
    <a-table
      row-key="id"
      size="small"
      :columns="billColumns"
      :data-source="bills"
      :loading="billLoading"
      :pagination="false"
      :row-selection="{ selectedRowKeys: checkedBills, onChange: onCheckBills }"
      :scroll="{ x: 860 }"
      :locale="{ emptyText: form.partnerId ? conf.noBills : conf.selectPartnerFirst }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'amount'">
          {{ formatMoney(record.amount) }}
          <div class="sub-text">{{ $t('fin.invoice.taxAmount') }} {{ formatMoney(record.taxAmount) }}</div>
        </template>
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
            @change="recalc"
          />
        </template>
      </template>
    </a-table>
    <template #footer>
      <div class="drawer-footer">
        <a-button @click="visible = false">{{ $t('common.action.cancel') }}</a-button>
        <a-button type="primary" :loading="saving" @click="submit">{{ $t('common.action.save') }}</a-button>
      </div>
    </template>
  </SubPage>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import { addInvoice, invoiceOpenBills } from '@/api/fin'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'
import { round2 } from '@/utils/number'
import { today } from '@/components/wms/wms'
import { formatMoney } from '@/components/fin/fin'
import { KINDS, billColumns as makeBillColumns, billRemaining, useInvoiceBills } from './invoice'
import { InvoiceType } from '@/constants/enums'

/** 登记发票：选择往来单位后列出未全部开票的单据，勾选并填写本次勾稽金额。通过 ref 调用 open()，成功后触发 success */
const props = defineProps({ kind: { type: String, required: true } })
const emit = defineEmits(['success'])
const { t } = useI18n()
const dict = useDict('fin_invoice_type')
const conf = computed(() => KINDS[props.kind])

const billColumns = makeBillColumns()
const visible = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({})
const { bills, billLoading, checkedBills, selectedBills, itemTotal, resetBills, fetchBills, onCheckBills: checkBills, billItems } = useInvoiceBills()

function open() {
  form.value = { kind: props.kind, partnerId: undefined, invoiceNo: '', invoiceType: InvoiceType.SPECIAL, invoiceDate: today(), amount: undefined, taxAmount: undefined, remark: '' }
  resetBills()
  visible.value = true
}

async function loadBills(partnerId) {
  resetBills()
  recalc()
  if (!partnerId) return
  await fetchBills(() => invoiceOpenBills(form.value.kind, partnerId))
}

function onCheckBills(keys) {
  checkBills(keys)
  recalc()
}

/** 按勾稽金额重新计算价税合计、税额（税额 = 单据税额 × 勾稽金额 / 单据金额） */
function recalc() {
  const rows = selectedBills.value
  if (!rows.length) {
    form.value.amount = undefined
    form.value.taxAmount = undefined
    return
  }
  form.value.amount = itemTotal.value
  form.value.taxAmount = round2(
    rows.reduce((s, b) => s + (Number(b.amount) ? (Number(b.taxAmount || 0) * (Number(b.apply) || 0)) / Number(b.amount) : 0), 0)
  )
}

async function submit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  const items = billItems()
  if (!items.length) {
    message.warning(t('fin.invoice.selectBills'))
    return
  }
  if (Math.abs(itemTotal.value - Number(form.value.amount)) >= 0.005) {
    message.warning(t('fin.invoice.totalMismatch', { amount: formatMoney(itemTotal.value) }))
    return
  }
  saving.value = true
  try {
    await addInvoice({ ...form.value, invoiceNo: form.value.invoiceNo.trim(), remark: form.value.remark || null, items })
    message.success(t('fin.invoice.registered'))
    visible.value = false
    emit('success')
  } finally {
    saving.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.bills-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 8px 0 12px;
}
.section-title {
  font-weight: 500;
}
.total {
  font-size: 16px;
  color: var(--ant-color-primary, #1677ff);
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.full {
  width: 100%;
}
</style>
