<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('common.field.name')" name="name">
          <a-input v-model:value="query.name" :placeholder="$t('workflow.list.processNamePh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('workflow.list.category')" name="category">
          <a-select v-model:value="query.category" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.wf_category)" />
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
          <template #icon><PlusOutlined /></template>{{ $t('workflow.model.add') }}
        </a-button>
        <span v-perm="'wf:model:remove'">
          <a-popconfirm
            :title="$t('workflow.model.confirmBatchDelete', { n: selectedKeys.length })"
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
                  <a-tooltip v-if="record.businessName" :title="$t('workflow.model.businessSubmit', { name: record.businessName })">
                    <a-tag color="blue" :bordered="false" class="biz-tag">{{ $t('workflow.model.businessDoc') }}</a-tag>
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
            <a-tag v-else :bordered="false">{{ $t('workflow.model.unpublished') }}</a-tag>
            <a-tag v-if="!record.designed" color="orange" :bordered="false">{{ $t('workflow.model.undesigned') }}</a-tag>
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
                <template #icon><NodeIndexOutlined /></template>{{ $t('workflow.model.design') }}
              </a-button>
              <a-button v-perm="'wf:model:edit'" type="link" size="small" @click="drawer.openEdit(record)">
                <template #icon><EditOutlined /></template>{{ $t('common.action.edit') }}
              </a-button>
              <a-dropdown v-if="moreActions(record).length">
                <a-button type="link" size="small">{{ $t('common.action.more') }}<DownOutlined /></a-button>
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
    <FormDrawer :drawer="drawer" :create-title="$t('workflow.model.add')" :edit-title="$t('workflow.model.edit')">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('workflow.list.processName')" name="name">
              <a-input v-model:value="form.name" :maxlength="64" :placeholder="$t('workflow.model.namePh')" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item
              :label="$t('workflow.model.key')"
              name="modelKey"
              :extra="form.version ? $t('workflow.model.keyLocked') : $t('workflow.model.keyExtra')"
            >
              <a-input v-model:value="form.modelKey" :maxlength="64" :placeholder="$t('workflow.model.keyPh')" :disabled="!!form.version" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('workflow.list.category')" name="category">
              <a-select v-model:value="form.category" :placeholder="$t('workflow.model.categoryPh')" allow-clear :options="toOptions(dict.wf_category)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('workflow.model.icon')" name="icon">
              <IconPicker v-model="form.icon" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('common.field.sort')" name="sort">
              <a-input-number v-model:value="form.sort" :min="0" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('common.field.description')" name="remark">
              <a-textarea v-model:value="form.remark" :rows="3" :maxlength="255" show-count :placeholder="$t('workflow.model.remarkPh')" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </FormDrawer>

    <ModelDesigner v-model:open="designerOpen" :model-id="designerId" @saved="load" />
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
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

const { t } = useI18n()

const userStore = useUserStore()
const dict = useDict('wf_category')

const columns = [
  { title: t('workflow.list.process'), key: 'name', width: 220 },
  { title: t('workflow.list.category'), key: 'category', width: 110, dict: 'wf_category' },
  { title: t('workflow.model.version'), key: 'version', width: 140 },
  { title: t('common.field.status'), key: 'status', width: 100 },
  { title: t('common.field.sort'), dataIndex: 'sort', width: 80 },
  { title: t('workflow.model.deployTime'), dataIndex: 'deployTime', width: 170 },
  { title: t('common.field.updateTime'), dataIndex: 'updateTime', width: 170 },
  { title: t('common.action.operation'), key: 'action', width: 240, fixed: 'right' }
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
  name: [{ required: true, whitespace: true, message: t('workflow.list.processNamePh') }],
  modelKey: [
    { required: true, message: t('workflow.model.keyRequired') },
    { pattern: /^[A-Za-z][A-Za-z0-9_]{0,63}$/, message: t('workflow.model.keyPattern') }
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
    actions.push({ key: 'deploy', label: t('workflow.model.deploy'), icon: CloudUploadOutlined, disabled: !record.designed })
  }
  if (userStore.hasPerm('wf:model:remove')) actions.push({ key: 'remove', label: t('common.action.delete'), icon: DeleteOutlined, danger: true })
  return actions
}

function onMoreAction(key, record) {
  if (key === 'deploy') {
    Modal.confirm({
      title: t('workflow.model.deployTitle', { name: record.name }),
      content: t('workflow.model.deployContent'),
      async onOk() {
        const version = await deployModel(record.id)
        message.success(t('workflow.model.deployed', { v: version }))
        load()
      }
    })
  }
  if (key === 'remove') {
    Modal.confirm({
      title: t('workflow.model.deleteTitle', { name: record.name }),
      content: t('workflow.model.deleteContent'),
      okType: 'danger',
      onOk: () => remove([record.id])
    })
  }
}

async function remove(ids) {
  await deleteModel(ids)
  message.success(t('common.msg.deleteSuccess'))
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
