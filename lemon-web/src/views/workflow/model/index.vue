<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item label="名称" name="name">
          <a-input v-model:value="query.name" placeholder="请输入流程名称" allow-clear />
        </a-form-item>
        <a-form-item label="分类" name="category">
          <a-select v-model:value="query.category" placeholder="全部" allow-clear :options="toOptions(dict.wf_category)" />
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
        <a-button v-perm="'wf:model:add'" type="primary" @click="drawer.openCreate()">
          <template #icon><PlusOutlined /></template>新增流程
        </a-button>
        <span v-perm="'wf:model:remove'">
          <a-popconfirm
            :title="`确定删除选中的 ${selectedKeys.length} 个流程？`"
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
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :row-selection="rowSelection"
        :scroll="{ x: 1200, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <div class="model-name">
              <span class="model-icon"><MenuIcon :name="record.icon || 'AuditOutlined'" /></span>
              <div class="model-name-text">
                <span>{{ record.name }}</span>
                <span class="sub-text">
                  {{ record.modelKey }}
                  <a-tooltip v-if="record.businessName" :title="`由「${record.businessName}」业务单据提交审批`">
                    <a-tag color="blue" :bordered="false" class="biz-tag">业务单据</a-tag>
                  </a-tooltip>
                </span>
              </div>
            </div>
          </template>
          <template v-else-if="column.key === 'category'">
            <DictTag type="wf_category" :value="record.category" />
          </template>
          <template v-else-if="column.key === 'version'">
            <a-tag v-if="record.version" color="green" :bordered="false">v{{ record.version }}</a-tag>
            <a-tag v-else :bordered="false">未发布</a-tag>
            <a-tag v-if="!record.designed" color="orange" :bordered="false">未设计</a-tag>
          </template>
          <template v-else-if="column.key === 'status'">
            <StatusSwitch
              :value="record.status"
              :name="record.name"
              :api="(s) => changeModelStatus(record.id, s)"
              :disabled="!userStore.hasPerm('wf:model:edit')"
              @changed="(s) => (record.status = s)"
            />
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button v-perm="['wf:model:edit', 'wf:model:deploy']" type="link" size="small" @click="openDesigner(record)">
                <template #icon><NodeIndexOutlined /></template>设计
              </a-button>
              <a-button v-perm="'wf:model:edit'" type="link" size="small" @click="drawer.openEdit(record)">
                <template #icon><EditOutlined /></template>编辑
              </a-button>
              <a-dropdown v-if="moreActions(record).length">
                <a-button type="link" size="small">更多<DownOutlined /></a-button>
                <template #overlay>
                  <a-menu @click="({ key }) => onMoreAction(key, record)">
                    <a-menu-item v-for="a in moreActions(record)" :key="a.key" :danger="a.danger" :disabled="a.disabled">
                      <component :is="a.icon" /> {{ a.label }}
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增 / 编辑基本信息 -->
    <FormDrawer :drawer="drawer" create-title="新增流程" edit-title="编辑流程">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item label="流程名称" name="name">
              <a-input v-model:value="form.name" :maxlength="64" placeholder="如：采购申请" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item
              label="流程标识"
              name="modelKey"
              :extra="form.version ? '已发布的流程不能修改标识' : '英文字母开头，发布后不能修改'"
            >
              <a-input v-model:value="form.modelKey" :maxlength="64" placeholder="如：leave" :disabled="!!form.version" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="分类" name="category">
              <a-select v-model:value="form.category" placeholder="请选择分类" allow-clear :options="toOptions(dict.wf_category)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="图标" name="icon">
              <IconPicker v-model="form.icon" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="排序" name="sort">
              <a-input-number v-model:value="form.sort" :min="0" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="说明" name="remark">
              <a-textarea v-model:value="form.remark" :rows="3" :maxlength="255" show-count placeholder="展示在发起申请页面" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </FormDrawer>

    <ModelDesigner v-model:open="designerOpen" :model-id="designerId" @saved="load" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import {
  CloudUploadOutlined,
  DeleteOutlined,
  DownOutlined,
  EditOutlined,
  NodeIndexOutlined,
  PlusOutlined
} from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import IconPicker from '@/components/IconPicker.vue'
import MenuIcon from '@/components/MenuIcon.vue'
import SearchForm from '@/components/SearchForm.vue'
import StatusSwitch from '@/components/StatusSwitch.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import ModelDesigner from './components/ModelDesigner.vue'
import { addModel, changeModelStatus, deleteModel, deployModel, getModel, pageModel, updateModel } from '@/api/workflow/model'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'

const userStore = useUserStore()
const dict = useDict('wf_category')

const columns = [
  { title: '流程', key: 'name', width: 220 },
  { title: '分类', key: 'category', width: 100, dict: 'wf_category' },
  { title: '版本', key: 'version', width: 140 },
  { title: '状态', key: 'status', width: 90 },
  { title: '排序', dataIndex: 'sort', width: 70 },
  { title: '发布时间', dataIndex: 'deployTime', width: 170 },
  { title: '更新时间', dataIndex: 'updateTime', width: 170 },
  { title: '操作', key: 'action', width: 210, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'workflow-model')
const showSearch = ref(true)

const query = reactive({ name: undefined, category: undefined })
const { list, loading, size, pagination, selectedKeys, rowSelection, load, search, onTableChange, exportAll } = useTable(
  pageModel,
  query
)

function resetQuery() {
  Object.assign(query, { name: undefined, category: undefined })
  search()
}

onMounted(load)

// 基本信息
const emptyForm = () => ({
  id: undefined,
  modelKey: '',
  name: '',
  category: undefined,
  icon: undefined,
  sort: 0,
  remark: '',
  version: undefined
})
const rules = {
  name: [{ required: true, whitespace: true, message: '请输入流程名称' }],
  modelKey: [
    { required: true, message: '请输入流程标识' },
    { pattern: /^[A-Za-z][A-Za-z0-9_]{0,63}$/, message: '以字母开头，只能包含字母、数字、下划线' }
  ]
}
const pick = ({ modelKey, name, category, icon, sort, remark }) => ({ modelKey, name, category, icon, sort, remark })
const drawer = useFormDrawer({
  emptyForm,
  fetch: (record) => getModel(record.id),
  create: (data) => addModel(data),
  update: (id, data) => updateModel(id, data),
  toPayload: pick,
  onSuccess: load
})
const { formRef, form } = drawer

// 设计
const designerOpen = ref(false)
const designerId = ref()
function openDesigner(record) {
  designerId.value = record.id
  designerOpen.value = true
}

function moreActions(record) {
  const actions = []
  if (userStore.hasPerm('wf:model:deploy')) {
    actions.push({ key: 'deploy', label: '发布', icon: CloudUploadOutlined, disabled: !record.designed })
  }
  if (userStore.hasPerm('wf:model:remove')) actions.push({ key: 'remove', label: '删除', icon: DeleteOutlined, danger: true })
  return actions
}

function onMoreAction(key, record) {
  if (key === 'deploy') {
    Modal.confirm({
      title: `发布「${record.name}」？`,
      content: '发布后生成新版本，新发起的申请使用新版本，进行中的申请仍按原版本执行。',
      async onOk() {
        const version = await deployModel(record.id)
        message.success(`发布成功，当前版本 v${version}`)
        load()
      }
    })
  }
  if (key === 'remove') {
    Modal.confirm({
      title: `确定删除流程「${record.name}」？`,
      content: '删除后不能再发起该流程，已发起的申请不受影响。',
      okType: 'danger',
      onOk: () => remove([record.id])
    })
  }
}

async function remove(ids) {
  await deleteModel(ids)
  message.success('删除成功')
  selectedKeys.value = selectedKeys.value.filter((k) => !ids.includes(k))
  load()
}
</script>

<style scoped>
.model-name {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.model-icon {
  width: 32px;
  height: 32px;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  font-size: 16px;
  color: var(--app-primary);
  background: var(--app-primary-bg);
}
.model-name-text {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
  min-width: 0;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.full {
  width: 100%;
}
.biz-tag {
  margin-left: 6px;
  font-size: 11px;
  line-height: 16px;
  padding: 0 4px;
}
</style>
