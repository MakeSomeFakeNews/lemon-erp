<template>
  <a-drawer
    :open="open"
    :title="dictType ? `字典数据：${dictType.name}` : '字典数据'"
    width="min(880px, 100vw)"
    destroy-on-close
    @close="emit('update:open', false)"
  >
    <template #extra>
      <a-typography-text v-if="dictType" code>{{ dictType.type }}</a-typography-text>
    </template>

    <TableToolbar
      :exporter="exportAll" v-model:size="size" @refresh="load">
      <a-input-search
        v-model:value="query.label"
        placeholder="搜索标签"
        allow-clear
        style="width: 200px"
        @search="search"
      />
      <a-button v-perm="'sys:dict:add'" type="primary" @click="openForm()">
        <template #icon><PlusOutlined /></template>新增数据
      </a-button>
      <span v-perm="'sys:dict:remove'">
        <a-popconfirm
          :title="`确定删除选中的 ${selectedKeys.length} 条数据？`"
          :disabled="!selectedKeys.length"
          @confirm="remove(selectedKeys)"
        >
          <a-button danger :disabled="!selectedKeys.length">
            <template #icon><DeleteOutlined /></template>批量删除
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
      :row-selection="rowSelection"
      :scroll="{ x: 700 }"
      @change="onTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'label'">
          <a-tag :color="tagColor(record.tagColor)" :bordered="false">{{ record.label }}</a-tag>
        </template>
        <template v-else-if="column.key === 'value'">
          <a-typography-text code>{{ record.value }}</a-typography-text>
        </template>
        <template v-else-if="column.key === 'status'">
          <DictTag type="sys_common_status" :value="record.status" />
        </template>
        <template v-else-if="column.key === 'action'">
          <span class="table-actions">
            <a-button v-perm="'sys:dict:edit'" type="link" size="small" @click="openForm(record)">
              <template #icon><EditOutlined /></template>编辑
            </a-button>
            <span v-perm="'sys:dict:remove'">
              <a-popconfirm title="确定删除该数据？" @confirm="remove([record.id])">
                <a-button type="link" size="small" danger>
                  <template #icon><DeleteOutlined /></template>删除
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
      :title="form.id ? '编辑字典数据' : '新增字典数据'"
      :confirm-loading="saving"
      width="560px"
      destroy-on-close
      @ok="submit"
    >
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item label="数据标签" name="label">
              <a-input v-model:value="form.label" placeholder="显示文字，如 启用" :maxlength="100" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="数据键值" name="value">
              <a-input v-model:value="form.value" placeholder="存储值，如 1" :maxlength="100" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="标签颜色" name="tagColor">
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
                预览：
                <a-tag :color="tagColor(form.tagColor)" :bordered="false">{{ form.label || '标签' }}</a-tag>
              </div>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="排序" name="sort">
              <a-input-number v-model:value="form.sort" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="状态" name="status">
              <a-radio-group v-model:value="form.status" :options="toOptions(dict.sys_common_status, true)" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" name="remark">
              <a-input v-model:value="form.remark" :maxlength="255" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </a-drawer>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import { addDictData, deleteDictData, pageDictData, updateDictData } from '@/api/system/dict'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { useDictStore } from '@/store/dict'
import { toOptions } from '@/utils/dict'

const props = defineProps({
  open: Boolean,
  dictType: Object
})
const emit = defineEmits(['update:open'])

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

const columns = [
  { title: '数据标签', key: 'label', width: 160 },
  { title: '数据键值', key: 'value', width: 120 },
  { title: '排序', dataIndex: 'sort', width: 70 },
  { title: '状态', key: 'status', width: 90, dict: 'sys_common_status' },
  { title: '备注', dataIndex: 'remark', ellipsis: true },
  { title: '操作', key: 'action', width: 150, fixed: 'right' }
]

const query = reactive({ dictType: undefined, label: undefined })
const { list, loading, size, pagination, selectedKeys, rowSelection, load, search, onTableChange, exportAll } = useTable(
  pageDictData,
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
  label: [{ required: true, message: '请输入数据标签' }],
  value: [{ required: true, message: '请输入数据键值' }]
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
    id ? await updateDictData(id, data) : await addDictData(data)
    dictStore.clear(props.dictType.type)
    message.success('保存成功')
    formOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(ids) {
  await deleteDictData(ids)
  dictStore.clear(props.dictType.type)
  message.success('删除成功')
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
.preview {
  margin-top: 12px;
  color: var(--app-text-3);
}
</style>
