<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('system.tenant.name')" name="name">
          <a-input v-model:value="query.name" :placeholder="$t('system.tenant.name')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.sys_common_status, true)" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <a-button v-perm="'sys:tenant:add'" type="primary" @click="drawer.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('system.tenant.add') }}
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
            <a-tag v-if="record.platform" color="gold" class="platform-tag">{{ $t('system.tenant.platform') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'packageName'">
            {{ record.platform ? $t('system.tenant.unlimited') : record.packageName || '-' }}
          </template>
          <template v-else-if="column.key === 'contact'">
            <div>{{ record.contactName || '-' }}</div>
            <div class="sub-text">{{ record.contactPhone }}</div>
          </template>
          <template v-else-if="column.key === 'account'">
            <span :class="{ over: record.accountLimit != null && record.accountCount >= record.accountLimit }">{{ record.accountCount }}</span>
            <span class="sub-text"> / {{ record.accountLimit ?? $t('system.tenant.unlimited') }}</span>
          </template>
          <template v-else-if="column.key === 'expireTime'">
            <span v-if="!record.expireTime" class="sub-text">{{ $t('system.tenant.permanent') }}</span>
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
                <template #icon><EditOutlined /></template>{{ $t('common.action.edit') }}
              </a-button>
              <span v-if="!record.platform" v-perm="'sys:tenant:remove'">
                <a-popconfirm
                  :title="$t('system.tenant.confirmDelete', { name: record.name })"
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

    <FormDrawer :drawer="drawer" :create-title="$t('system.tenant.add')" :edit-title="$t('system.tenant.edit')" :width="form.id ? undefined : 'min(880px, 100vw)'">
      <a-alert
        v-if="!form.id"
        type="info"
        show-icon
        class="form-tip"
        :message="$t('system.tenant.createTip')"
      />
      <a-alert v-else-if="form.platform" type="info" show-icon class="form-tip" :message="$t('system.tenant.platformTip')" />
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('system.tenant.name')" name="name" :extra="$t('system.tenant.nameTip')">
              <a-input v-model:value="form.name" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col v-if="!form.id" :xs="24" :sm="12">
            <a-form-item :label="$t('system.tenant.mode')" name="mode" :extra="$t('system.tenant.modeTip')">
              <a-radio-group v-model:value="form.mode" button-style="solid">
                <a-radio-button value="template">{{ $t('system.tenant.byTemplate') }}</a-radio-button>
                <a-radio-button value="package">{{ $t('system.tenant.byPackage') }}</a-radio-button>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col v-if="form.id || form.mode === 'package'" :xs="24" :sm="12">
            <a-form-item :label="$t('system.tenant.package')" name="packageId" :extra="$t('system.tenant.packageTip')">
              <a-select v-model:value="form.packageId" :options="packageOptions" :disabled="form.platform" :placeholder="form.platform ? $t('system.tenant.unlimited') : $t('common.placeholder.select')" />
            </a-form-item>
          </a-col>
          <a-col v-if="!form.id && form.mode === 'template'" :span="24">
            <a-form-item name="menuIds" :extra="$t('system.tenant.templateTip')">
              <a-form-item-rest>
                <FeaturePicker v-if="catalog" v-model="form.menuIds" :catalog="catalog" @industry="(i) => (form.templateName = i?.name || '')" />
              </a-form-item-rest>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('system.tenant.contact')" name="contactName"><a-input v-model:value="form.contactName" :maxlength="64" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('system.dept.contactPhone')" name="contactPhone"><a-input v-model:value="form.contactPhone" :maxlength="32" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('system.tenant.expireTime')" name="expireTime" :extra="$t('system.tenant.expireTip')">
              <a-date-picker
                v-model:value="form.expireTime"
                show-time
                value-format="YYYY-MM-DD HH:mm:ss"
                :disabled="form.platform"
                :placeholder="$t('system.tenant.permanent')"
                class="full"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('system.tenant.accountLimit')" name="accountLimit" :extra="$t('system.tenant.accountLimitTip')">
              <a-input-number v-model:value="form.accountLimit" :min="1" :disabled="form.platform" :placeholder="$t('system.tenant.unlimited')" class="full" />
            </a-form-item>
          </a-col>
          <template v-if="!form.id">
            <a-col :xs="24" :sm="12">
              <a-form-item :label="$t('system.tenant.adminUsername')" name="adminUsername">
                <a-input v-model:value="form.adminUsername" :maxlength="64" :placeholder="$t('system.tenant.adminPlaceholder')" autocomplete="off" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12">
              <a-form-item :label="$t('system.tenant.adminPassword')" name="adminPassword" :extra="$t('system.tenant.adminPasswordTip')">
                <a-input-password v-model:value="form.adminPassword" autocomplete="new-password" />
              </a-form-item>
            </a-col>
          </template>
          <a-col v-else :xs="24" :sm="12">
            <a-form-item :label="$t('system.tenant.adminUsername')">
              <a-input :value="form.adminUsername" disabled />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('common.field.remark')" name="remark"><a-textarea v-model:value="form.remark" :rows="3" :maxlength="255" /></a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </FormDrawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import FeaturePicker from '@/components/system/FeaturePicker.vue'
import SearchForm from '@/components/SearchForm.vue'
import StatusSwitch from '@/components/StatusSwitch.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import {
  addTenant,
  changeTenantStatus,
  deleteTenant,
  getTenant,
  pageTenant,
  tenantFeatures,
  tenantPackageOptions,
  updateTenant
} from '@/api/system/tenant'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import { useAction } from '@/composables/useAction'

const { t } = useI18n()
const userStore = useUserStore()
const dict = useDict('sys_common_status')
const showSearch = ref(true)

const columns = [
  { title: t('system.tenant.name'), key: 'name', width: 200 },
  { title: t('system.tenant.package'), key: 'packageName', width: 140 },
  { title: t('system.tenant.contact'), key: 'contact', width: 150 },
  { title: t('system.tenant.accountCol'), key: 'account', width: 130, align: 'right' },
  { title: t('system.tenant.expireTime'), key: 'expireTime', width: 120 },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 200, ellipsis: true },
  { title: t('common.field.createTime'), dataIndex: 'createTime', width: 170 },
  { title: t('common.field.status'), key: 'status', width: 90 },
  { title: t('common.action.operation'), key: 'action', width: 160, fixed: 'right' }
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
const catalog = ref()
async function loadPackages() {
  const [packages, features] = await Promise.all([tenantPackageOptions(), catalog.value || tenantFeatures()])
  packageOptions.value = packages.map((p) => ({ label: p.name, value: p.id }))
  catalog.value = features
}

const rules = {
  name: [{ required: true, whitespace: true, message: t('system.tenant.inputName') }],
  packageId: [{ validator: (_, v) => (form.value.platform || form.value.mode === 'template' || v ? Promise.resolve() : Promise.reject(t('system.tenant.selectPackage'))) }],
  menuIds: [{ validator: (_, v) => (form.value.mode !== 'template' || v?.length ? Promise.resolve() : Promise.reject(t('system.tenant.selectTemplate'))) }],
  adminUsername: [{ required: true, whitespace: true, message: t('system.tenant.inputAdmin') }],
  adminPassword: [{ required: true, message: t('system.tenant.inputAdminPassword') }]
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
    platform: false,
    mode: 'template',
    menuIds: [],
    templateName: ''
  }),
  beforeOpen: loadPackages,
  fetch: (record) => getTenant(record.id),
  create: addTenant,
  update: updateTenant,
  toPayload: ({ id, platform, packageName, accountCount, adminUserId, adminUsername, adminPassword, mode, menuIds, templateName, ...rest }) => {
    if (id) return rest
    const payload = { ...rest, adminUsername, adminPassword }
    // 按行业模板：不传套餐，后端按菜单匹配或新建套餐
    return mode === 'template' ? { ...payload, packageId: null, menuIds, templateName: templateName || t('system.tenant.custom') } : payload
  },
  onSuccess: load
})
const { formRef, form } = drawer

const remove = useAction(deleteTenant, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
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
