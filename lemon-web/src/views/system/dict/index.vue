<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item label="字典名称" name="name">
          <a-input v-model:value="query.name" placeholder="请输入字典名称" allow-clear />
        </a-form-item>
        <a-form-item label="字典类型" name="type">
          <a-input v-model:value="query.type" placeholder="请输入字典类型" allow-clear />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select
            v-model:value="query.status"
            placeholder="全部"
            allow-clear
            :options="toOptions(dict.sys_common_status, true)"
          />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        :exporter="exportAll"
        v-model:size="size"
        v-model:checked="checked"
        v-model:show-search="showSearch"
        :columns="columns"
        @refresh="load"
      >
        <a-button v-perm="'sys:dict:add'" type="primary" @click="openForm()">
          <template #icon><PlusOutlined /></template>新增字典
        </a-button>
        <span v-perm="'sys:dict:remove'">
          <a-popconfirm
            :title="`确定删除选中的 ${selectedKeys.length} 个字典？字典数据会一并删除`"
            :disabled="!selectedKeys.length"
            @confirm="remove(selectedKeys)"
          >
            <a-button danger :disabled="!selectedKeys.length">
              <template #icon><DeleteOutlined /></template>批量删除
            </a-button>
          </a-popconfirm>
        </span>
        <template #extra>
          <a-tooltip title="清理前端字典缓存">
            <a-button type="text" @click="clearCache">
              <template #icon><ClearOutlined /></template>
            </a-button>
          </a-tooltip>
        </template>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :row-selection="rowSelection"
        :scroll="{ x: 900, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'type'">
            <a @click="openData(record)">
              <a-typography-text code>{{ record.type }}</a-typography-text>
            </a>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="sys_common_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openData(record)">
                <template #icon><UnorderedListOutlined /></template>数据
              </a-button>
              <a-button v-perm="'sys:dict:edit'" type="link" size="small" @click="openForm(record)">
                <template #icon><EditOutlined /></template>编辑
              </a-button>
              <span v-perm="'sys:dict:remove'">
                <a-popconfirm title="确定删除该字典？字典数据会一并删除" @confirm="remove([record.id])">
                  <a-button type="link" size="small" danger>
                    <template #icon><DeleteOutlined /></template>删除
                  </a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 字典类型表单 -->
    <a-drawer
      v-model:open="formOpen"
      :title="form.id ? '编辑字典' : '新增字典'"
      width="min(560px, 100vw)"
      destroy-on-close
    >
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item label="字典名称" name="name">
              <a-input v-model:value="form.name" placeholder="如 用户性别" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="字典类型" name="type">
              <a-input v-model:value="form.type" placeholder="如 sys_user_sex" :maxlength="100" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="状态" name="status">
              <a-radio-group v-model:value="form.status" :options="toOptions(dict.sys_common_status, true)" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" name="remark">
              <a-textarea v-model:value="form.remark" :rows="3" :maxlength="255" show-count />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <template #footer>
        <div class="drawer-footer">
          <a-button @click="formOpen = false">取消</a-button>
          <a-button type="primary" :loading="saving" @click="submit">保存</a-button>
        </div>
      </template>
    </a-drawer>

    <DataDrawer v-model:open="dataOpen" :dict-type="current" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  ClearOutlined,
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  UnorderedListOutlined
} from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import DataDrawer from './DataDrawer.vue'
import { addDictType, deleteDictType, pageDictType, updateDictType } from '@/api/system/dict'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { useDictStore } from '@/store/dict'
import { toOptions } from '@/utils/dict'

const dictStore = useDictStore()
const dict = useDict('sys_common_status')

const columns = [
  { title: '字典名称', dataIndex: 'name', width: 180 },
  { title: '字典类型', key: 'type', width: 220 },
  { title: '状态', key: 'status', width: 90, dict: 'sys_common_status' },
  { title: '备注', dataIndex: 'remark', ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 230, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns)
const showSearch = ref(true)

const query = reactive({ name: undefined, type: undefined, status: undefined })
const { list, loading, size, pagination, selectedKeys, rowSelection, load, search, onTableChange, exportAll } = useTable(
  pageDictType,
  query
)
onMounted(load)

function resetQuery() {
  Object.assign(query, { name: undefined, type: undefined, status: undefined })
  search()
}

function clearCache() {
  dictStore.clear()
  message.success('字典缓存已清理')
}

// 表单
const formOpen = ref(false)
const saving = ref(false)
const formRef = ref()
const emptyForm = () => ({ id: undefined, name: '', type: '', status: 1, remark: '' })
const form = ref(emptyForm())
let originalType = ''
const rules = {
  name: [{ required: true, message: '请输入字典名称' }],
  type: [
    { required: true, message: '请输入字典类型' },
    { pattern: /^[a-z][a-z0-9_]*$/, message: '小写字母开头，仅包含小写字母、数字、下划线' }
  ]
}

function openForm(record) {
  form.value = record ? { ...record } : emptyForm()
  originalType = record?.type || ''
  formOpen.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const { id, createTime, updateTime, ...data } = form.value
    id ? await updateDictType(id, data) : await addDictType(data)
    // 类型编码或状态变化都会影响字典数据，清掉新旧两个类型的缓存
    dictStore.clear(originalType)
    dictStore.clear(data.type)
    message.success('保存成功')
    formOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(ids) {
  await deleteDictType(ids)
  dictStore.clear()
  message.success('删除成功')
  selectedKeys.value = []
  load()
}

// 字典数据
const dataOpen = ref(false)
const current = ref(null)
function openData(record) {
  current.value = record
  dataOpen.value = true
}
</script>
