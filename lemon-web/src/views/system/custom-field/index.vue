<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <div class="toolbar">
        <a-space wrap>
          <span>业务类型</span>
          <a-select v-model:value="bizType" :options="typeOptions" allow-clear placeholder="全部" class="type-select" @change="load" />
          <span class="sub-text">为单据、主数据扩展字段，在对应的表单中填写；勾选「列表显示」的在列表中显示</span>
        </a-space>
        <a-button v-perm="'sys:customField:edit'" type="primary" @click="drawer.openCreate({ bizType })">
          <template #icon><PlusOutlined /></template>新增字段
        </a-button>
      </div>
      <a-table row-key="id" size="middle" :columns="columns" :data-source="list" :loading="loading" :pagination="false" :scroll="{ x: 1000, y: '100%' }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'bizType'">{{ typeName(record.bizType) }}</template>
          <template v-else-if="column.key === 'fieldType'"><DictTag type="sys_custom_field_type" :value="record.fieldType" /></template>
          <template v-else-if="column.key === 'flags'">
            <a-tag v-if="record.required === 1" color="red" :bordered="false">必填</a-tag>
            <a-tag v-if="record.showInList === 1" color="blue" :bordered="false">列表显示</a-tag>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 0 ? 'default' : 'green'" :bordered="false">{{ record.status === 0 ? '停用' : '启用' }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <span v-perm="'sys:customField:edit'" class="table-actions">
              <a-button type="link" size="small" @click="drawer.openEdit(record)">编辑</a-button>
              <a-popconfirm :title="`删除字段「${record.label}」？已填写的值一并删除。`" @confirm="remove(record.id)">
                <a-button type="link" danger size="small">删除</a-button>
              </a-popconfirm>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" create-title="新增自定义字段" edit-title="编辑自定义字段">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item label="业务类型" name="bizType">
          <a-select v-model:value="form.bizType" :options="typeOptions" :disabled="!!form.id" show-search option-filter-prop="label" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="字段标识" name="fieldKey" extra="字母开头，保存后不能修改">
              <a-input v-model:value="form.fieldKey" :maxlength="32" :disabled="!!form.id" placeholder="如 brand" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="显示名称" name="label">
              <a-input v-model:value="form.label" :maxlength="32" placeholder="如 品牌" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="类型" name="fieldType">
              <a-select v-model:value="form.fieldType" :options="toOptions(dict.sys_custom_field_type)" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="排序" name="sort">
              <a-input-number v-model:value="form.sort" :min="0" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item v-if="form.fieldType === 'SELECT'" label="选项" name="options" extra="逗号或换行分隔">
          <a-textarea v-model:value="form.options" :rows="3" :maxlength="1000" />
        </a-form-item>
        <a-space :size="24" class="flags">
          <a-checkbox :checked="form.required === 1" @update:checked="(v) => (form.required = v ? 1 : 0)">必填</a-checkbox>
          <a-checkbox :checked="form.showInList === 1" @update:checked="(v) => (form.showInList = v ? 1 : 0)">列表显示</a-checkbox>
          <a-checkbox :checked="form.status !== 0" @update:checked="(v) => (form.status = v ? 1 : 0)">启用</a-checkbox>
        </a-space>
        <a-form-item label="说明" name="remark">
          <a-input v-model:value="form.remark" :maxlength="255" placeholder="显示为输入提示" />
        </a-form-item>
      </a-form>
    </FormDrawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import { addCustomField, allCustomFields, customFieldTypes, deleteCustomField, updateCustomField } from '@/api/system/customField'
import { clearCustomFieldCache } from '@/composables/useCustomFields'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { toOptions } from '@/utils/dict'

/** 自定义字段：按业务类型维护扩展字段的定义 */
const dict = useDict('sys_custom_field_type')
const types = ref([])
const typeOptions = computed(() => types.value.map((t) => ({ value: t.code, label: t.name })))
const typeName = (code) => types.value.find((t) => t.code === code)?.name || code
const bizType = ref()
const list = ref([])
const loading = ref(false)
const columns = [
  { title: '业务类型', key: 'bizType', width: 130 },
  { title: '显示名称', dataIndex: 'label', width: 140 },
  { title: '字段标识', dataIndex: 'fieldKey', width: 130 },
  { title: '类型', key: 'fieldType', width: 100 },
  { title: '选项', dataIndex: 'options', ellipsis: true },
  { title: '属性', key: 'flags', width: 150 },
  { title: '排序', dataIndex: 'sort', width: 70 },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 130, fixed: 'right' }
]
async function load() {
  loading.value = true
  try {
    list.value = await allCustomFields(bizType.value || undefined)
  } finally {
    loading.value = false
  }
}
onMounted(async () => {
  types.value = await customFieldTypes()
  load()
})

const rules = {
  bizType: [{ required: true, message: '请选择业务类型' }],
  fieldKey: [
    { required: true, message: '请填写字段标识' },
    { pattern: /^[A-Za-z][A-Za-z0-9_]{0,31}$/, message: '字母开头的字母、数字、下划线' }
  ],
  label: [{ required: true, whitespace: true, message: '请填写显示名称' }],
  fieldType: [{ required: true, message: '请选择类型' }]
}
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, bizType: undefined, fieldKey: '', label: '', fieldType: 'TEXT', options: '', required: 0, showInList: 0, sort: 0, status: 1, remark: '' }),
  create: addCustomField,
  update: updateCustomField,
  toPayload: ({ bizType: b, fieldKey, label, fieldType, options, required, showInList, sort, status, remark }) => ({
    bizType: b,
    fieldKey,
    label,
    fieldType,
    options: fieldType === 'SELECT' ? options : null,
    required,
    showInList,
    sort,
    status,
    remark: remark || null
  }),
  onSuccess: () => {
    clearCustomFieldCache()
    load()
  }
})
const { formRef, form } = drawer
async function remove(id) {
  await deleteCustomField(id)
  clearCustomFieldCache()
  message.success('删除成功')
  load()
}
</script>

<style scoped>
.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 12px;
}
.type-select {
  width: 180px;
}
.flags {
  margin-bottom: 16px;
}
.full {
  width: 100%;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
