<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item label="租户名称" name="name">
          <a-input v-model:value="query.name" placeholder="租户名称" allow-clear />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear :options="toOptions(dict.sys_common_status, true)" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <a-button v-perm="'sys:tenant:add'" type="primary" @click="drawer.openCreate()">
          <template #icon><PlusOutlined /></template>新增租户
        </a-button>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1300, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <span>{{ record.name }}</span>
            <a-tag v-if="record.platform" color="gold" class="platform-tag">平台</a-tag>
          </template>
          <template v-else-if="column.key === 'packageName'">
            {{ record.platform ? '不限' : record.packageName || '-' }}
          </template>
          <template v-else-if="column.key === 'contact'">
            <div>{{ record.contactName || '-' }}</div>
            <div class="sub-text">{{ record.contactPhone }}</div>
          </template>
          <template v-else-if="column.key === 'account'">
            <span :class="{ over: record.accountLimit != null && record.accountCount >= record.accountLimit }">{{ record.accountCount }}</span>
            <span class="sub-text"> / {{ record.accountLimit ?? '不限' }}</span>
          </template>
          <template v-else-if="column.key === 'expireTime'">
            <span v-if="!record.expireTime" class="sub-text">长期</span>
            <span v-else :class="{ over: isExpired(record) }">{{ record.expireTime.slice(0, 10) }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <StatusSwitch
              :value="record.status"
              :name="record.name"
              :api="(s) => changeTenantStatus(record.id, s)"
              :disabled="record.platform || !userStore.hasPerm('sys:tenant:edit')"
              @changed="(s) => (record.status = s)"
            />
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button v-perm="'sys:tenant:edit'" type="link" size="small" @click="drawer.openEdit(record)">
                <template #icon><EditOutlined /></template>编辑
              </a-button>
              <span v-if="!record.platform" v-perm="'sys:tenant:remove'">
                <a-popconfirm
                  :title="`确定删除租户「${record.name}」？删除后无法登录，业务数据保留。`"
                  @confirm="remove(record.id)"
                >
                  <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" create-title="新增租户" edit-title="编辑租户">
      <a-alert
        v-if="!form.id"
        type="info"
        show-icon
        class="form-tip"
        message="新建租户时会开通根部门、租户管理员账号和内置岗位角色，并复制会计科目、班次、流程模型等基础数据。"
      />
      <a-alert v-else-if="form.platform" type="info" show-icon class="form-tip" message="平台租户不受套餐、到期时间和账号数量限制。" />
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item label="租户名称" name="name" extra="用户登录时填写">
              <a-input v-model:value="form.name" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="套餐" name="packageId" extra="决定租户可用的菜单和功能">
              <a-select v-model:value="form.packageId" :options="packageOptions" :disabled="form.platform" :placeholder="form.platform ? '不限' : '请选择'" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="联系人" name="contactName"><a-input v-model:value="form.contactName" :maxlength="64" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="联系电话" name="contactPhone"><a-input v-model:value="form.contactPhone" :maxlength="32" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="到期时间" name="expireTime" extra="为空表示长期有效；到期后无法登录">
              <a-date-picker
                v-model:value="form.expireTime"
                show-time
                value-format="YYYY-MM-DD HH:mm:ss"
                :disabled="form.platform"
                placeholder="长期"
                class="full"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="账号数量上限" name="accountLimit" extra="为空不限">
              <a-input-number v-model:value="form.accountLimit" :min="1" :disabled="form.platform" placeholder="不限" class="full" />
            </a-form-item>
          </a-col>
          <template v-if="!form.id">
            <a-col :xs="24" :sm="12">
              <a-form-item label="管理员账号" name="adminUsername">
                <a-input v-model:value="form.adminUsername" :maxlength="64" placeholder="如：admin" autocomplete="off" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12">
              <a-form-item label="管理员密码" name="adminPassword" extra="首次登录后需修改">
                <a-input-password v-model:value="form.adminPassword" autocomplete="new-password" />
              </a-form-item>
            </a-col>
          </template>
          <a-col v-else :xs="24" :sm="12">
            <a-form-item label="管理员账号">
              <a-input :value="form.adminUsername" disabled />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" name="remark"><a-textarea v-model:value="form.remark" :rows="3" :maxlength="255" /></a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </FormDrawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import SearchForm from '@/components/SearchForm.vue'
import StatusSwitch from '@/components/StatusSwitch.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import {
  addTenant,
  changeTenantStatus,
  deleteTenant,
  getTenant,
  pageTenant,
  tenantPackageOptions,
  updateTenant
} from '@/api/system/tenant'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'

const userStore = useUserStore()
const dict = useDict('sys_common_status')
const showSearch = ref(true)

const columns = [
  { title: '租户名称', key: 'name', width: 200 },
  { title: '套餐', key: 'packageName', width: 140 },
  { title: '联系人', key: 'contact', width: 150 },
  { title: '账号 / 上限', key: 'account', width: 110, align: 'right' },
  { title: '到期时间', key: 'expireTime', width: 120 },
  { title: '备注', dataIndex: 'remark', width: 200, ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', width: 170 },
  { title: '状态', key: 'status', width: 90 },
  { title: '操作', key: 'action', width: 140, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'system-tenant')

const emptyQuery = () => ({ name: undefined, status: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pageTenant, query)
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}
onMounted(load)

const isExpired = (record) => record.expireTime && new Date(record.expireTime.replace(' ', 'T')) <= new Date()

const packageOptions = ref([])
async function loadPackages() {
  packageOptions.value = (await tenantPackageOptions()).map((p) => ({ label: p.name, value: p.id }))
}

const rules = {
  name: [{ required: true, whitespace: true, message: '请输入租户名称' }],
  packageId: [{ validator: (_, v) => (form.value.platform || v ? Promise.resolve() : Promise.reject('请选择套餐')) }],
  adminUsername: [{ required: true, whitespace: true, message: '请输入管理员账号' }],
  adminPassword: [{ required: true, message: '请输入管理员密码' }]
}
const drawer = useFormDrawer({
  emptyForm: () => ({
    id: undefined,
    name: '',
    packageId: undefined,
    contactName: '',
    contactPhone: '',
    expireTime: null,
    accountLimit: null,
    adminUsername: '',
    adminPassword: '',
    status: 1,
    remark: '',
    platform: false
  }),
  beforeOpen: loadPackages,
  fetch: (record) => getTenant(record.id),
  create: addTenant,
  update: updateTenant,
  toPayload: ({ id, platform, packageName, accountCount, adminUserId, adminUsername, adminPassword, ...rest }) =>
    id ? rest : { ...rest, adminUsername, adminPassword },
  onSuccess: load
})
const { formRef, form } = drawer

async function remove(id) {
  await deleteTenant(id)
  message.success('删除成功')
  load()
}
</script>

<style scoped>
.platform-tag {
  margin-left: 6px;
}
.form-tip {
  margin-bottom: 16px;
}
.over {
  color: var(--status-critical);
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.full {
  width: 100%;
}
</style>
