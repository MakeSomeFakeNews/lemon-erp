<template>
  <a-drawer
    :open="open"
    :title="dictType ? $t('system.dict.dataTitle', { name: dictType.name }) : $t('system.dict.dictData')"
    width="min(880px, 100vw)"
    destroy-on-close
    @close="emit('update:open', false)"
  >
    <template #extra>
      <a-typography-text v-if="dictType" code>{{ dictType.type }}</a-typography-text>
    </template>

    <a-alert v-if="dictType" type="info" show-icon class="kind-tip" :message="kindTip" />

    <TableToolbar
      :exporter="exportAll" v-model:size="size" @refresh="load">
      <a-input-search
        v-model:value="query.label"
        :placeholder="$t('system.dict.searchLabel')"
        allow-clear
        style="width: 200px"
        @search="search"
      />
      <a-button v-if="!isSystem" v-perm="perms.add" type="primary" @click="openForm()">
        <template #icon><PlusOutlined /></template>{{ $t('system.dict.addData') }}
      </a-button>
      <span v-if="!isSystem" v-perm="perms.remove">
        <a-popconfirm
          :title="$t('system.dict.confirmDeleteDataN', { n: selectedKeys.length })"
          :disabled="!selectedKeys.length"
          @confirm="remove(selectedKeys)"
        >
          <a-button danger :disabled="!selectedKeys.length">
            <template #icon><DeleteOutlined /></template>{{ $t('common.action.batchDelete') }}
          </a-button>
        </a-popconfirm>
      </span>
    </TableToolbar>

    <a-table
      row-key="id"
      :size="size"
      :columns="columns"
      :data-source="list"
      :loading="loading"
      :pagination="pagination"
      :row-selection="isSystem ? undefined : rowSelection"
      :scroll="{ x: 760 }"
      @change="onTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'label'">
          <a-tag :color="tagColor(record.tagColor)" :bordered="false">{{ record.label }}</a-tag>
        </template>
        <template v-else-if="column.key === 'value'">
          <a-typography-text code>{{ record.value }}</a-typography-text>
          <a-tooltip v-if="record.builtin === 1 && !isSystem" :title="$t('system.dict.builtinTip')">
            <a-tag :bordered="false" class="builtin-tag">{{ $t('system.dict.builtin') }}</a-tag>
          </a-tooltip>
        </template>
        <template v-else-if="column.key === 'status'">
          <DictTag type="sys_common_status" :value="record.status" />
        </template>
        <template v-else-if="column.key === 'action'">
          <span class="table-actions">
            <a-button v-perm="perms.edit" type="link" size="small" @click="openForm(record)">
              <template #icon><EditOutlined /></template>{{ $t('common.action.edit') }}
            </a-button>
            <span v-if="!isSystem && record.builtin !== 1" v-perm="perms.remove">
              <a-popconfirm :title="$t('system.dict.confirmDeleteData')" @confirm="remove([record.id])">
                <a-button type="link" size="small" danger>
                  <template #icon><DeleteOutlined /></template>{{ $t('common.action.delete') }}
                </a-button>
              </a-popconfirm>
            </span>
          </span>
        </template>
      </template>
    </a-table>

    <!-- 数据表单 -->
    <a-modal
      v-model:open="formOpen"
      :title="form.id ? $t('system.dict.editData') : $t('system.dict.addDataTitle')"
      :confirm-loading="saving"
      width="560px"
      destroy-on-close
      @ok="submit"
    >
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('system.dict.label')" name="label">
              <a-input v-model:value="form.label" :placeholder="$t('system.dict.labelPlaceholder')" :maxlength="100" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('system.dict.value')" name="value">
              <a-input v-model:value="form.value" :placeholder="$t('system.dict.valuePlaceholder')" :maxlength="100"
                :disabled="isSystem || form.builtin === 1" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('system.dict.tagColor')" name="tagColor">
              <div class="color-palette">
                <span
                  v-for="c in COLORS"
                  :key="c"
                  :class="['color-option', { active: (form.tagColor || 'default') === c }]"
                  @click="form.tagColor = c"
                >
                  <a-tag :color="tagColor(c)" :bordered="false">{{ c }}</a-tag>
                </span>
              </div>
              <div class="preview">
                {{ $t('system.dict.previewLabel') }}
                <a-tag :color="tagColor(form.tagColor)" :bordered="false">{{ form.label || $t('system.dict.tag') }}</a-tag>
              </div>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('common.field.sort')" name="sort">
              <a-input-number v-model:value="form.sort" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('common.field.status')" name="status">
              <a-radio-group v-model:value="form.status" :options="toOptions(dict.sys_common_status, true)" :disabled="isSystem" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('common.field.remark')" name="remark">
              <a-input v-model:value="form.remark" :maxlength="255" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </a-drawer>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import { addBizDictData, addDictData, deleteBizDictData, deleteDictData, pageBizDictData, pageDictData, updateBizDictData, updateDictData } from '@/api/system/dict'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { useDictStore } from '@/store/dict'
import { toOptions } from '@/utils/dict'

/**
 * 字典数据：系统字典只能改名称、颜色、排序、备注；业务字典可以新增取值，内置取值（程序依赖）不能删除、不能改键值。
 * biz 为「业务字典」页面（当前租户自己的取值，权限 sys:bizdict:*），否则为平台的「字典管理」
 */
const props = defineProps({
  open: Boolean,
  dictType: Object,
  biz: Boolean
})
const emit = defineEmits(['update:open'])

const { t } = useI18n()
const dictStore = useDictStore()
const dict = useDict('sys_common_status')

/** ant-design-vue Tag 预设颜色 */
const COLORS = [
  'default',
  'processing',
  'success',
  'warning',
  'error',
  'blue',
  'green',
  'orange',
  'red',
  'purple',
  'cyan',
  'gold',
  'magenta',
  'geekblue',
  'lime',
  'volcano'
]
const tagColor = (c) => (c && c !== 'default' ? c : undefined)

const isSystem = computed(() => props.dictType?.kind === 'SYSTEM')
const perms = computed(() => (props.biz
  ? { add: 'sys:bizdict:edit', edit: 'sys:bizdict:edit', remove: 'sys:bizdict:edit' }
  : { add: 'sys:dict:add', edit: 'sys:dict:edit', remove: 'sys:dict:remove' }))
const api = computed(() => (props.biz
  ? { page: pageBizDictData, add: addBizDictData, update: updateBizDictData, remove: deleteBizDictData }
  : { page: pageDictData, add: addDictData, update: updateDictData, remove: deleteDictData }))
const kindTip = computed(() => {
  if (isSystem.value) return t('system.dict.systemTip')
  return props.biz ? t('system.dict.businessTip') : t('system.dict.platformBusinessTip')
})

const columns = [
  { title: t('system.dict.label'), key: 'label', width: 160 },
  { title: t('system.dict.value'), key: 'value', width: 180 },
  { title: t('common.field.sort'), dataIndex: 'sort', width: 70 },
  { title: t('common.field.status'), key: 'status', width: 90, dict: 'sys_common_status' },
  { title: t('common.field.remark'), dataIndex: 'remark', ellipsis: true },
  { title: t('common.action.operation'), key: 'action', width: 160, fixed: 'right' }
]

const query = reactive({ dictType: undefined, label: undefined })
const { list, loading, size, pagination, selectedKeys, rowSelection, load, search, onTableChange, exportAll } = useTable(
  (params) => api.value.page(params),
  query
)

watch(
  () => [props.open, props.dictType?.type],
  ([open, type]) => {
    if (open && type) {
      query.dictType = type
      query.label = undefined
      selectedKeys.value = []
      search()
    }
  }
)

// 表单
const formOpen = ref(false)
const saving = ref(false)
const formRef = ref()
const emptyForm = () => ({
  id: undefined,
  label: '',
  value: '',
  tagColor: 'default',
  sort: (pagination.total || 0) + 1,
  status: 1,
  remark: ''
})
const form = ref(emptyForm())
const rules = {
  label: [{ required: true, message: t('system.dict.inputLabel') }],
  value: [
    { required: true, message: t('system.dict.inputValue') },
    // 业务字典的键值存入各业务表的编码字段，与后端 SysDictService 的格式一致
    { validator: (_, v) => (isSystem.value || form.value.builtin === 1 || !v || /^[A-Z][A-Z0-9_]{0,15}$/.test(v) ? Promise.resolve() : Promise.reject(t('system.dict.bizValuePattern'))) }
  ]
}

function openForm(record) {
  form.value = record ? { ...record, tagColor: record.tagColor || 'default' } : emptyForm()
  formOpen.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const { id, createTime, updateTime, ...rest } = form.value
    const data = { ...rest, dictType: props.dictType.type }
    const { builtin, ...payload } = data
    id ? await api.value.update(id, payload) : await api.value.add(payload)
    dictStore.clear(props.dictType.type)
    message.success(t('common.msg.saveSuccess'))
    formOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(ids) {
  await api.value.remove(ids)
  dictStore.clear(props.dictType.type)
  message.success(t('common.msg.deleteSuccess'))
  selectedKeys.value = []
  load()
}
</script>

<style scoped>
.color-palette {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.color-option {
  padding: 2px;
  border-radius: 6px;
  border: 1px solid transparent;
  cursor: pointer;
  transition: border-color 0.15s;
}
.color-option .ant-tag {
  margin: 0;
  cursor: pointer;
}
.color-option.active {
  border-color: var(--app-primary);
}
.kind-tip {
  margin-bottom: 12px;
}
.builtin-tag {
  margin-left: 6px;
}
.preview {
  margin-top: 12px;
  color: var(--app-text-3);
}
</style>
