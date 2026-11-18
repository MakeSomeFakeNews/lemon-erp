<template>
  <!-- 指定科目：现金科目、银行科目、其他货币资金科目（凭证科目设置中的科目范围用途） -->
  <a-modal v-model:open="visible" :title="$t('fin.account.designateTitle')" :confirm-loading="saving" @ok="submit">
    <a-alert type="info" show-icon class="tip" :message="$t('fin.account.designateTip')" />
    <a-form layout="vertical">
      <a-form-item v-for="r in ROLES" :key="r.role" :label="r.label" :extra="$t('fin.account.designateDefault', { code: r.defaultCode })">
        <AccountSelect v-model:value="form[r.role]" all allow-clear :placeholder="$t('fin.account.designatePh', { code: r.defaultCode })" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import AccountSelect from '@/components/fin/AccountSelect.vue'
import { accountRules, saveAccountRule } from '@/api/fin'

/**
 * 指定科目（参照用友「指定科目」）：资金账户的入账科目只能选这些科目下的末级科目，现金流量表、资金计划按此识别货币资金。
 * 保存到凭证科目设置的科目范围用途（CASH、BANK、OTHER_FUNDS），未选择的使用预置科目
 */
const emit = defineEmits(['saved'])
const { t } = useI18n()
const visible = ref(false)
const saving = ref(false)
const ROLES = [
  { role: 'CASH', label: t('fin.account.cashSubject'), defaultCode: '1001' },
  { role: 'BANK', label: t('fin.account.bankSubject'), defaultCode: '1002' },
  { role: 'OTHER_FUNDS', label: t('fin.account.otherFundsSubject'), defaultCode: '1012' }
]
const form = reactive({})
let original = {}

async function open() {
  const rules = await accountRules()
  ROLES.forEach((r) => {
    form[r.role] = rules.find((x) => x.role === r.role)?.accountId ?? null
    r.defaultCode = rules.find((x) => x.role === r.role)?.defaultCode || r.defaultCode
  })
  original = { ...form }
  visible.value = true
}

async function submit() {
  saving.value = true
  try {
    for (const r of ROLES) {
      if (form[r.role] !== original[r.role]) {
        await saveAccountRule(r.role, { accountId: form[r.role] || null })
      }
    }
    message.success(t('common.msg.saveSuccess'))
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
  margin-bottom: 12px;
}
</style>
