<template>
  <a-drawer :open="open" :title="$t(kind === 'customer' ? 'scm.portalAccount.dealerTitle' : 'scm.portalAccount.supplierTitle', { name: partner?.name || '' })" width="min(720px, 100vw)" destroy-on-close @close="emit('update:open', false)">
    <a-alert
      type="info"
      show-icon
      class="block"
      :message="kind === 'customer'
        ? $t('scm.portalAccount.dealerTip')
        : $t('scm.portalAccount.supplierTip')"
    />
    <a-table row-key="userId" size="small" :columns="columns" :data-source="accounts" :loading="loading" :pagination="false">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-switch
            :checked="record.status === 1"
            :checked-children="$t('common.status.enabled')"
            :un-checked-children="$t('common.status.disabled')"
            @change="(v) => changeStatus(record, v)"
          />
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button type="link" size="small" @click="openReset(record)">{{ $t('scm.portalAccount.resetPassword') }}</a-button>
        </template>
      </template>
    </a-table>
    <div class="section-title">{{ $t('scm.portalAccount.openAccount') }}</div>
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="12">
        <a-col :span="12">
          <a-form-item :label="$t('scm.portalAccount.username')" name="username">
            <a-input v-model:value="form.username" :maxlength="64" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('scm.portalAccount.initPassword')" name="password" :extra="$t('scm.portalAccount.initPasswordTip')">
            <a-input-password v-model:value="form.password" :maxlength="64" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('scm.portalAccount.contact')" name="nickname">
            <a-input v-model:value="form.nickname" :maxlength="64" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('common.field.phone')" name="phone">
            <a-input v-model:value="form.phone" :maxlength="32" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-button type="primary" :loading="saving" @click="create">{{ $t('scm.portalAccount.open') }}</a-button>
    </a-form>

    <a-modal v-model:open="resetOpen" :title="$t('scm.portalAccount.resetTitle', { name: resetting?.username || '' })" @ok="submitReset">
      <a-input-password v-model:value="newPassword" :maxlength="64" :placeholder="$t('scm.portalAccount.newPassword')" />
    </a-modal>
  </a-drawer>
</template>

<script setup>
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { addPortalAccount, changePortalAccountStatus, portalAccounts, resetPortalAccountPassword } from '@/api/scm/srm'

/** 门户账号：开通、停用、重置密码。kind：supplier 供应商门户 / customer 经销商门户 */
const props = defineProps({
  open: Boolean,
  kind: { type: String, default: 'supplier' },
  partner: Object
})
const emit = defineEmits(['update:open'])
const { t } = useI18n()

const columns = [
  { title: t('scm.portalAccount.account'), dataIndex: 'username', width: 140 },
  { title: t('scm.portalAccount.contact'), dataIndex: 'nickname', width: 120 },
  { title: t('common.field.phone'), dataIndex: 'phone', width: 130 },
  { title: t('common.field.status'), key: 'status', width: 100 },
  { title: '', key: 'action', width: 130 }
]
const accounts = ref([])
const loading = ref(false)
async function load() {
  loading.value = true
  try {
    accounts.value = await portalAccounts(props.kind, props.partner.id)
  } finally {
    loading.value = false
  }
}
const emptyForm = () => ({ username: '', password: '', nickname: props.partner?.contact || '', phone: props.partner?.phone || '' })
const form = ref(emptyForm())
const formRef = ref()
watch(
  () => props.open,
  (v) => {
    if (v && props.partner) {
      form.value = emptyForm()
      load()
    }
  }
)
const rules = {
  username: [{ required: true, whitespace: true, message: t('scm.portalAccount.usernameRequired') }],
  password: [{ required: true, message: t('scm.portalAccount.passwordRequired') }],
  nickname: [{ required: true, whitespace: true, message: t('scm.portalAccount.contactRequired') }]
}
const saving = ref(false)
async function create() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    await addPortalAccount(props.kind, props.partner.id, form.value)
    message.success(t('scm.portalAccount.opened'))
    form.value = emptyForm()
    load()
  } finally {
    saving.value = false
  }
}
async function changeStatus(record, checked) {
  await changePortalAccountStatus(props.kind, props.partner.id, record.userId, checked ? 1 : 0)
  record.status = checked ? 1 : 0
  message.success(checked ? t('scm.portalAccount.enabledMsg') : t('scm.portalAccount.disabledMsg'))
}
const resetOpen = ref(false)
const resetting = ref()
const newPassword = ref('')
function openReset(record) {
  resetting.value = record
  newPassword.value = ''
  resetOpen.value = true
}
async function submitReset() {
  if (!newPassword.value) return message.warning(t('scm.portalAccount.newPasswordRequired'))
  await resetPortalAccountPassword(props.kind, props.partner.id, resetting.value.userId, newPassword.value)
  message.success(t('scm.portalAccount.resetDone'))
  resetOpen.value = false
}
</script>

<style scoped>
.block {
  margin-bottom: 16px;
}
.section-title {
  margin: 16px 0 8px;
  font-weight: 600;
}
</style>
