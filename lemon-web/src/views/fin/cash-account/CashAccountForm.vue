<template>
  <FormDrawer :drawer="drawer" :create-title="$t('fin.cashAccount.add')" :edit-title="$t('fin.cashAccount.edit')">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-alert v-if="locked" type="info" show-icon class="tip" :message="$t('fin.cashAccount.lockedTip')" />
      <a-row :gutter="16">
        <a-col :xs="24" :sm="14">
          <a-form-item :label="$t('fin.cashAccount.name')" name="name" :extra="$t('fin.cashAccount.nameExtra')">
            <a-input v-model:value="form.name" :maxlength="64" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="10">
          <a-form-item :label="$t('fin.cashAccount.type')" name="type">
            <a-select v-model:value="form.type" :options="toOptions(dict.fin_cash_account_type)" :disabled="locked" @change="!form.used && (form.accountId = undefined)" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item :label="$t('fin.cashAccount.subject')" name="accountId" :extra="subjectExtra">
        <AccountSelect :key="form.type" v-model:value="form.accountId" :roles="subjectRoles" :disabled="locked || form.used" :placeholder="$t('fin.cashAccount.subjectPh')" />
      </a-form-item>
      <a-row v-if="!isCash" :gutter="16">
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('fin.cashAccount.bankName')" name="bankName">
            <a-input v-model:value="form.bankName" :maxlength="64" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('fin.cashAccount.bankAccountNo')" name="bankAccountNo">
            <a-input v-model:value="form.bankAccountNo" :maxlength="64" />
          </a-form-item>
        </a-col>
      </a-row>

      <a-divider orientation="left" class="divider">{{ $t('fin.cashAccount.openingSection') }}</a-divider>
      <a-form-item :label="$t('fin.cashAccount.enableDate')" name="enableDate" :extra="$t('fin.cashAccount.enableDateExtra')">
        <a-date-picker v-model:value="form.enableDate" value-format="YYYY-MM-DD" :disabled="locked" allow-clear class="full" />
      </a-form-item>
      <a-row v-if="form.enableDate" :gutter="16">
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('fin.cashAccount.journalOpening')" name="journalOpening" :extra="ledgerText">
            <a-input-number v-model:value="form.journalOpening" :precision="2" :disabled="locked" class="full" />
          </a-form-item>
        </a-col>
        <a-col v-if="!isCash" :xs="24" :sm="12">
          <a-form-item :label="$t('fin.cashAccount.statementOpening')" name="statementOpening" :extra="$t('fin.cashAccount.statementOpeningExtra')">
            <a-input-number v-model:value="form.statementOpening" :precision="2" :disabled="locked" class="full" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-alert v-if="unbalancedHint" type="warning" show-icon class="tip" :message="unbalancedHint" />

      <a-row :gutter="16">
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('common.field.sort')" name="sort">
            <a-input-number v-model:value="form.sort" :min="0" :precision="0" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="16">
          <a-form-item :label="$t('common.field.remark')" name="remark">
            <a-input v-model:value="form.remark" :maxlength="255" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-checkbox v-if="!form.id" v-model:checked="form.enable">{{ $t('fin.cashAccount.enableNow') }}</a-checkbox>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import FormDrawer from '@/components/FormDrawer.vue'
import AccountSelect from '@/components/fin/AccountSelect.vue'
import { addCashAccount, cashAccountLedgerBalance, updateCashAccount } from '@/api/fin'
import { formatMoney } from '@/components/fin/fin'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { toOptions } from '@/utils/dict'
import { STATUS_ENABLED } from '@/utils/constants'
import { CashAccountType } from '@/constants/enums'

/** 新增 / 修改资金账户：启用中的账户只能改名称、开户行、账号、排序、备注；入账科目被使用后不能更换 */
const emit = defineEmits(['saved'])
const { t } = useI18n()
const dict = useDict('fin_cash_account_type')

const drawer = useFormDrawer({
  emptyForm: () => ({
    id: undefined,
    name: '',
    type: CashAccountType.BANK,
    accountId: undefined,
    bankName: '',
    bankAccountNo: '',
    enableDate: undefined,
    journalOpening: 0,
    statementOpening: 0,
    sort: 0,
    remark: '',
    enable: true,
    status: undefined,
    used: false
  }),
  create: addCashAccount,
  update: updateCashAccount,
  toPayload: (f) => ({
    name: f.name,
    type: f.type,
    accountId: f.accountId,
    bankName: f.bankName || null,
    bankAccountNo: f.bankAccountNo || null,
    enableDate: f.enableDate || null,
    journalOpening: f.journalOpening ?? 0,
    statementOpening: f.statementOpening ?? 0,
    sort: f.sort ?? 0,
    remark: f.remark || null,
    enable: !f.id && f.enable
  }),
  onSuccess: () => emit('saved')
})
const { formRef, form } = drawer

const isCash = computed(() => form.value.type === CashAccountType.CASH)
/** 启用中的账户锁定类型、入账科目、启用日期和期初 */
const locked = computed(() => form.value.status === STATUS_ENABLED)
/** 现金账户的入账科目在现金科目下，其他类型在银行科目或其他货币资金科目下（指定科目） */
const subjectRoles = computed(() => (isCash.value ? ['CASH'] : ['BANK', 'OTHER_FUNDS']))
const subjectExtra = computed(() => (form.value.used ? t('fin.cashAccount.subjectUsed') : isCash.value ? t('fin.cashAccount.subjectExtraCash') : t('fin.cashAccount.subjectExtraBank')))

/** 入账科目在启用日期之前的账面余额，供录入日记账期初参考 */
const ledger = ref(null)
watch(
  () => [form.value.accountId, form.value.enableDate],
  async ([accountId, date]) => {
    ledger.value = null
    if (!accountId || !date) return
    ledger.value = await cashAccountLedgerBalance(accountId, date)
  }
)
const ledgerText = computed(() => (ledger.value == null ? t('fin.cashAccount.journalOpeningExtra') : t('fin.cashAccount.ledgerBalance', { amount: formatMoney(ledger.value) })))
/** 新增银行类账户时日记账期初与对账单期初不同：需要先保存、录入期初未达账后再启用 */
const unbalancedHint = computed(() => {
  const f = form.value
  if (isCash.value || !f.enableDate || Number(f.journalOpening || 0) === Number(f.statementOpening || 0)) return ''
  return t('fin.cashAccount.unbalancedHint')
})
watch(unbalancedHint, (v) => {
  if (v && !form.value.id) form.value.enable = false
})

const rules = {
  name: [{ required: true, whitespace: true, message: t('fin.cashAccount.nameRequired') }],
  type: [{ required: true, message: t('fin.cashAccount.typeRequired') }],
  accountId: [{ required: true, message: t('fin.cashAccount.subjectRequired') }]
}

defineExpose({ openCreate: () => drawer.openCreate(), openEdit: (record) => drawer.openEdit({ ...record, enable: false }) })
</script>

<style scoped>
.tip {
  margin-bottom: 12px;
}
.full {
  width: 100%;
}
.divider {
  font-size: 14px;
  margin: 4px 0 12px;
}
</style>
