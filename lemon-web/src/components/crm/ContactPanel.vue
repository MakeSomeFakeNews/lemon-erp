<template>
  <div>
    <div class="bar">
      <span class="sub-text">{{ $t('crm.contactPanel.primaryTip') }} <StarFilled class="star" /> {{ $t('crm.contactPanel.primaryTipSuffix') }}</span>
      <a-button v-if="editable" type="primary" size="small" @click="openForm()">
        <template #icon><PlusOutlined /></template>{{ $t('crm.contactPanel.add') }}
      </a-button>
    </div>
    <a-table row-key="id" size="small" :columns="columns" :data-source="list" :loading="loading" :pagination="false" :scroll="{ x: 760 }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          <StarFilled v-if="record.isPrimary === 1" class="star" /> {{ record.name }}
          <div v-if="record.title" class="sub-text">{{ record.title }}</div>
        </template>
        <template v-else-if="column.key === 'phone'">
          <div>{{ record.phone || '-' }}</div>
          <div v-if="record.email" class="sub-text">{{ record.email }}</div>
        </template>
        <template v-else-if="column.key === 'action'">
          <span v-if="editable" class="table-actions">
            <a-button type="link" size="small" @click="openForm(record)"><EditOutlined /></a-button>
            <a-popconfirm :title="$t('crm.contactPanel.confirmDelete', { name: record.name })" @confirm="remove(record.id)">
              <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
            </a-popconfirm>
          </span>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="formOpen" :title="form.id ? $t('crm.contactPanel.editTitle') : $t('crm.contactPanel.add')" :confirm-loading="saving" destroy-on-close @ok="submit">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="$t('crm.contactPanel.name')" name="name"><a-input v-model:value="form.name" :maxlength="64" /></a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('crm.contactPanel.title')" name="title"><a-input v-model:value="form.title" :maxlength="64" /></a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('crm.contactPanel.phone')" name="phone"><a-input v-model:value="form.phone" :maxlength="32" /></a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('crm.contactPanel.email')" name="email"><a-input v-model:value="form.email" :maxlength="128" /></a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('crm.contactPanel.wechat')" name="wechat"><a-input v-model:value="form.wechat" :maxlength="64" /></a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('crm.contactPanel.birthday')" name="birthday">
              <a-date-picker v-model:value="form.birthday" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item name="isPrimary">
              <a-checkbox :checked="form.isPrimary === 1" @change="(e) => (form.isPrimary = e.target.checked ? 1 : 0)">{{ $t('crm.contactPanel.setPrimary') }}</a-checkbox>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('common.field.remark')" name="remark"><a-input v-model:value="form.remark" :maxlength="255" /></a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { DeleteOutlined, EditOutlined, PlusOutlined, StarFilled } from '@ant-design/icons-vue'
import { addContact, deleteContact, listContacts, updateContact } from '@/api/crm'

/** 客户的联系人：负责人或有客户修改权限的用户可以维护 */
const props = defineProps({
  customerId: { type: Number, required: true },
  editable: Boolean
})
const emit = defineEmits(['changed'])
const { t } = useI18n()

const columns = [
  { title: t('crm.contactPanel.name'), key: 'name', width: 160 },
  { title: t('crm.contactPanel.phoneEmail'), key: 'phone', width: 200 },
  { title: t('crm.contactPanel.wechat'), dataIndex: 'wechat', width: 120 },
  { title: t('crm.contactPanel.birthday'), dataIndex: 'birthday', width: 110 },
  { title: t('common.field.remark'), dataIndex: 'remark', ellipsis: true },
  { title: '', key: 'action', width: 90, fixed: 'right' }
]
const list = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    list.value = await listContacts(props.customerId)
  } finally {
    loading.value = false
  }
}
onMounted(load)
watch(() => props.customerId, load)

const formOpen = ref(false)
const formRef = ref()
const form = ref({})
const saving = ref(false)
const rules = { name: [{ required: true, whitespace: true, message: t('crm.contactPanel.nameRequired') }], email: [{ type: 'email', message: t('common.rule.email') }] }

function openForm(record) {
  form.value = record ? { ...record } : { name: '', isPrimary: list.value.length ? 0 : 1 }
  formOpen.value = true
}

async function submit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    const { id, createTime, updateTime, ...rest } = form.value
    const data = { ...rest, customerId: props.customerId }
    id ? await updateContact(id, data) : await addContact(data)
    message.success(t('common.msg.saveSuccess'))
    formOpen.value = false
    load()
    emit('changed')
  } finally {
    saving.value = false
  }
}

async function remove(id) {
  await deleteContact(id)
  message.success(t('common.msg.deleteSuccess'))
  load()
  emit('changed')
}
</script>

<style scoped>
.bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.star {
  color: #faad14;
}
.full {
  width: 100%;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
