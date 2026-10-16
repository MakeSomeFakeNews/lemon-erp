<template>
  <a-drawer :open="open" :title="`${kind === 'customer' ? '经销商' : '供应商'}门户账号 - ${partner?.name || ''}`" width="min(720px, 100vw)" destroy-on-close @close="emit('update:open', false)">
    <a-alert
      type="info"
      show-icon
      class="block"
      :message="kind === 'customer'
        ? '经销商使用门户账号登录（SaaS 模式下租户名称与本公司相同），按价目表订货、查看本经销商的订单和返利余额、上报渠道库存和销量。'
        : '供应商使用门户账号登录（SaaS 模式下租户名称与本公司相同），只能看到本供应商的订单、送货通知、对账单、招标和质量索赔。'"
    />
    <a-table row-key="userId" size="small" :columns="columns" :data-source="accounts" :loading="loading" :pagination="false">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-switch
            :checked="record.status === 1"
            checked-children="启用"
            un-checked-children="停用"
            @change="(v) => changeStatus(record, v)"
          />
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button type="link" size="small" @click="openReset(record)">重置密码</a-button>
        </template>
      </template>
    </a-table>
    <div class="section-title">开通账号</div>
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="12">
        <a-col :span="12">
          <a-form-item label="登录账号" name="username">
            <a-input v-model:value="form.username" :maxlength="64" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="初始密码" name="password" extra="首次登录后提示修改">
            <a-input-password v-model:value="form.password" :maxlength="64" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="联系人" name="nickname">
            <a-input v-model:value="form.nickname" :maxlength="64" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="手机号" name="phone">
            <a-input v-model:value="form.phone" :maxlength="32" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-button type="primary" :loading="saving" @click="create">开通</a-button>
    </a-form>

    <a-modal v-model:open="resetOpen" :title="`重置密码 - ${resetting?.username || ''}`" @ok="submitReset">
      <a-input-password v-model:value="newPassword" :maxlength="64" placeholder="新密码" />
    </a-modal>
  </a-drawer>
</template>

<script setup>
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { addPortalAccount, changePortalAccountStatus, portalAccounts, resetPortalAccountPassword } from '@/api/scm/srm'

/** 门户账号：开通、停用、重置密码。kind：supplier 供应商门户 / customer 经销商门户 */
const props = defineProps({
  open: Boolean,
  kind: { type: String, default: 'supplier' },
  partner: Object
})
const emit = defineEmits(['update:open'])

const columns = [
  { title: '账号', dataIndex: 'username', width: 140 },
  { title: '联系人', dataIndex: 'nickname', width: 120 },
  { title: '手机号', dataIndex: 'phone', width: 130 },
  { title: '状态', key: 'status', width: 90 },
  { title: '', key: 'action', width: 90 }
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
  username: [{ required: true, whitespace: true, message: '请填写登录账号' }],
  password: [{ required: true, message: '请填写初始密码' }],
  nickname: [{ required: true, whitespace: true, message: '请填写联系人' }]
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
    message.success('已开通')
    form.value = emptyForm()
    load()
  } finally {
    saving.value = false
  }
}
async function changeStatus(record, checked) {
  await changePortalAccountStatus(props.kind, props.partner.id, record.userId, checked ? 1 : 0)
  record.status = checked ? 1 : 0
  message.success(checked ? '已启用' : '已停用')
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
  if (!newPassword.value) return message.warning('请填写新密码')
  await resetPortalAccountPassword(props.kind, props.partner.id, resetting.value.userId, newPassword.value)
  message.success('已重置，原登录会话已下线')
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
