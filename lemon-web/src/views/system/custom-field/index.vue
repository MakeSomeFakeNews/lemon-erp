<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <div class="toolbar">
        <a-space wrap>
          <span>{{ $t('system.customField.bizType') }}</span>
          <a-select v-model:value="bizType" :options="typeOptions" allow-clear :placeholder="$t('common.placeholder.all')" class="type-select" @change="load" />
          <span class="sub-text">{{ $t('system.customField.hint') }}</span>
        </a-space>
        <a-button v-perm="'sys:customField:edit'" type="primary" @click="drawer.openCreate({ bizType })">
          <template #icon><PlusOutlined /></template>{{ $t('system.customField.addField') }}
        </a-button>
      </div>
      <a-table row-key="id" size="middle" :columns="columns" :data-source="list" :loading="loading" :pagination="false" :scroll="{ x: scrollX, y: '100%' }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'bizType'">{{ typeName(record.bizType) }}</template>
          <template v-else-if="column.key === 'fieldType'"><DictTag type="sys_custom_field_type" :value="record.fieldType" /></template>
          <template v-else-if="column.key === 'flags'">
            <a-tag v-if="record.required === 1" color="red" :bordered="false">{{ $t('system.customField.required') }}</a-tag>
            <a-tag v-if="record.showInList === 1" color="blue" :bordered="false">{{ $t('system.customField.showInList') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 0 ? 'default' : 'green'" :bordered="false">{{ record.status === 0 ? $t('common.status.disabled') : $t('common.status.enabled') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <span v-perm="'sys:customField:edit'" class="table-actions">
              <a-button type="link" size="small" @click="drawer.openEdit(record)">{{ $t('common.action.edit') }}</a-button>
              <a-popconfirm :title="$t('system.customField.confirmDelete', { name: record.label })" @confirm="remove(record.id)">
                <a-button type="link" danger size="small">{{ $t('common.action.delete') }}</a-button>
              </a-popconfirm>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" :create-title="$t('system.customField.createTitle')" :edit-title="$t('system.customField.editTitle')">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item :label="$t('system.customField.bizType')" name="bizType">
          <a-select v-model:value="form.bizType" :options="typeOptions" :disabled="!!form.id" show-search option-filter-prop="label" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="$t('system.customField.fieldKey')" name="fieldKey" :extra="$t('system.customField.fieldKeyTip')">
              <a-input v-model:value="form.fieldKey" :maxlength="32" :disabled="!!form.id" :placeholder="$t('system.customField.fieldKeyPlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('system.customField.label')" name="label">
              <a-input v-model:value="form.label" :maxlength="32" :placeholder="$t('system.customField.labelPlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('common.field.type')" name="fieldType">
              <a-select v-model:value="form.fieldType" :options="toOptions(dict.sys_custom_field_type)" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('common.field.sort')" name="sort">
              <a-input-number v-model:value="form.sort" :min="0" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item v-if="form.fieldType === CustomFieldType.SELECT" :label="$t('system.customField.options')" name="options" :extra="$t('system.customField.optionsTip')">
          <a-textarea v-model:value="form.options" :rows="3" :maxlength="1000" />
        </a-form-item>
        <a-space :size="24" class="flags">
          <a-checkbox :checked="form.required === 1" @update:checked="(v) => (form.required = v ? 1 : 0)">{{ $t('system.customField.required') }}</a-checkbox>
          <a-checkbox :checked="form.showInList === 1" @update:checked="(v) => (form.showInList = v ? 1 : 0)">{{ $t('system.customField.showInList') }}</a-checkbox>
          <a-checkbox :checked="form.status !== 0" @update:checked="(v) => (form.status = v ? 1 : 0)">{{ $t('common.status.enabled') }}</a-checkbox>
        </a-space>
        <a-form-item :label="$t('common.field.description')" name="remark">
          <a-input v-model:value="form.remark" :maxlength="255" :placeholder="$t('system.customField.remarkPlaceholder')" />
        </a-form-item>
      </a-form>
    </FormDrawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import { addCustomField, allCustomFields, customFieldTypes, deleteCustomField, updateCustomField } from '@/api/system/customField'
import { clearCustomFieldCache } from '@/composables/useCustomFields'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { toOptions } from '@/utils/dict'

import { CustomFieldType } from '@/constants/enums'
/** 自定义字段：按业务类型维护扩展字段的定义 */
const { t } = useI18n()
const dict = useDict('sys_custom_field_type')
const types = ref([])
const typeOptions = computed(() => types.value.map((ty) => ({ value: ty.code, label: ty.name })))
const typeName = (code) => types.value.find((ty) => ty.code === code)?.name || code
const bizType = ref()
const list = ref([])
const loading = ref(false)
const columns = [
  { title: t('system.customField.bizType'), key: 'bizType', width: 140 },
  { title: t('system.customField.label'), dataIndex: 'label', width: 140 },
  { title: t('system.customField.fieldKey'), dataIndex: 'fieldKey', width: 130 },
  { title: t('common.field.type'), key: 'fieldType', width: 100 },
  { title: t('system.customField.options'), dataIndex: 'options', width: 220, ellipsis: true },
  { title: t('system.customField.flags'), key: 'flags', width: 190 },
  { title: t('common.field.sort'), dataIndex: 'sort', width: 70 },
  { title: t('common.field.status'), key: 'status', width: 90 },
  { title: t('common.action.operation'), key: 'action', width: 140, fixed: 'right' }
]
const scrollX = columns.reduce((sum, c) => sum + c.width, 0)
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
  bizType: [{ required: true, message: t('system.customField.selectBizType') }],
  fieldKey: [
    { required: true, message: t('system.customField.inputFieldKey') },
    { pattern: /^[A-Za-z][A-Za-z0-9_]{0,31}$/, message: t('system.customField.fieldKeyPattern') }
  ],
  label: [{ required: true, whitespace: true, message: t('system.customField.inputLabel') }],
  fieldType: [{ required: true, message: t('system.customField.selectType') }]
}
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, bizType: undefined, fieldKey: '', label: '', fieldType: CustomFieldType.TEXT, options: '', required: 0, showInList: 0, sort: 0, status: 1, remark: '' }),
  create: addCustomField,
  update: updateCustomField,
  toPayload: ({ bizType: b, fieldKey, label, fieldType, options, required, showInList, sort, status, remark }) => ({
    bizType: b,
    fieldKey,
    label,
    fieldType,
    options: fieldType === CustomFieldType.SELECT ? options : null,
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
  message.success(t('common.msg.deleteSuccess'))
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
