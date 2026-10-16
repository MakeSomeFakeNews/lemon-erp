<template>
  <div>
    <div class="bar">
      <span class="sub-text">首要联系人用 <StarFilled class="star" /> 标记</span>
      <a-button v-if="editable" type="primary" size="small" @click="openForm()">
        <template #icon><PlusOutlined /></template>新增联系人
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
            <a-popconfirm :title="`确定删除联系人「${record.name}」？`" @confirm="remove(record.id)">
              <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
            </a-popconfirm>
          </span>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="formOpen" :title="form.id ? '编辑联系人' : '新增联系人'" :confirm-loading="saving" destroy-on-close @ok="submit">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="姓名" name="name"><a-input v-model:value="form.name" :maxlength="64" /></a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="职务" name="title"><a-input v-model:value="form.title" :maxlength="64" /></a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="电话" name="phone"><a-input v-model:value="form.phone" :maxlength="32" /></a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="邮箱" name="email"><a-input v-model:value="form.email" :maxlength="128" /></a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="微信" name="wechat"><a-input v-model:value="form.wechat" :maxlength="64" /></a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="生日" name="birthday">
              <a-date-picker v-model:value="form.birthday" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item name="isPrimary">
              <a-checkbox :checked="form.isPrimary === 1" @change="(e) => (form.isPrimary = e.target.checked ? 1 : 0)">设为首要联系人</a-checkbox>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" name="remark"><a-input v-model:value="form.remark" :maxlength="255" /></a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined, EditOutlined, PlusOutlined, StarFilled } from '@ant-design/icons-vue'
import { addContact, deleteContact, listContacts, updateContact } from '@/api/crm'

/** 客户的联系人：负责人或有客户修改权限的用户可以维护 */
const props = defineProps({
  customerId: { type: Number, required: true },
  editable: Boolean
})
const emit = defineEmits(['changed'])

const columns = [
  { title: '姓名', key: 'name', width: 160 },
  { title: '电话 / 邮箱', key: 'phone', width: 200 },
  { title: '微信', dataIndex: 'wechat', width: 120 },
  { title: '生日', dataIndex: 'birthday', width: 110 },
  { title: '备注', dataIndex: 'remark', ellipsis: true },
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
const rules = { name: [{ required: true, whitespace: true, message: '请输入姓名' }], email: [{ type: 'email', message: '邮箱格式不正确' }] }

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
    message.success('保存成功')
    formOpen.value = false
    load()
    emit('changed')
  } finally {
    saving.value = false
  }
}

async function remove(id) {
  await deleteContact(id)
  message.success('删除成功')
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
