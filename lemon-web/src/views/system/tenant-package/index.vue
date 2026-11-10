<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-input-search v-model:value="keyword" :placeholder="$t('system.tenantPackage.name')" allow-clear class="search" @search="load" />
        <a-button v-perm="'sys:tenantPackage:add'" type="primary" @click="drawer.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('system.tenantPackage.add') }}
        </a-button>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: scrollX, y: '100%' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'default'">{{ record.status === 1 ? $t('common.status.enabled') : $t('common.status.disabled') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions
              :actions="[
                { type: 'edit', perm: 'sys:tenantPackage:edit', onClick: () => drawer.openEdit(record) },
                {
                  type: 'delete',
                  perm: 'sys:tenantPackage:remove',
                  disabled: record.tenantCount > 0,
                  confirm: $t('system.tenantPackage.confirmDelete', { name: record.name }),
                  onClick: () => remove(record.id)
                }
              ]"
            />
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" :create-title="$t('system.tenantPackage.add')" :edit-title="$t('system.tenantPackage.edit')" width="min(880px, 100vw)">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('system.tenantPackage.name')" name="name"><a-input v-model:value="form.name" :maxlength="64" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('common.field.status')" name="status" :extra="$t('system.tenantPackage.statusTip')">
              <a-radio-group v-model:value="form.status">
                <a-radio :value="1">{{ $t('common.status.enabled') }}</a-radio>
                <a-radio :value="0">{{ $t('common.status.disabled') }}</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('common.field.remark')" name="remark"><a-textarea v-model:value="form.remark" :rows="2" :maxlength="255" /></a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item
              :label="$t('system.tenantPackage.menus')"
              name="menuIds"
              :extra="$t('system.tenantPackage.menusTip')"
            >
              <!-- 树中的搜索框、勾选框不作为表单字段收集，校验仍按 menuIds -->
              <a-form-item-rest>
                <FeaturePicker v-if="catalog" v-model="form.menuIds" :catalog="catalog" :tree="menuTree" class="picker" />
                <div class="tree-label">{{ $t('system.tenantPackage.menuDetail') }} <span class="sub-text">{{ $t('system.tenantPackage.menuDetailTip') }}</span></div>
                <CheckTree v-model="form.menuIds" :tree="menuTree" check-ancestors height="420px" />
              </a-form-item-rest>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </FormDrawer>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { PlusOutlined } from '@ant-design/icons-vue'
import CheckTree from '@/components/tree/CheckTree.vue'
import FeaturePicker from '@/components/system/FeaturePicker.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import {
  addTenantPackage,
  deleteTenantPackage,
  getTenantPackage,
  listTenantPackage,
  tenantFeatures,
  tenantPackageMenuTree,
  updateTenantPackage
} from '@/api/system/tenant'
import { useColumns } from '@/composables/useColumns'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { translateMenus } from '@/store/user'
import { useAction } from '@/composables/useAction'

const { t } = useI18n()
const columns = [
  { title: t('system.tenantPackage.name'), dataIndex: 'name', width: 200 },
  { title: t('system.tenantPackage.tenantCount'), dataIndex: 'tenantCount', width: 90, align: 'right' },
  { title: t('common.field.remark'), dataIndex: 'remark', ellipsis: true },
  { title: t('common.field.createTime'), dataIndex: 'createTime', width: 170 },
  { title: t('common.field.status'), key: 'status', width: 90 },
  actionColumn(2, [t('common.action.edit')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'system-tenant-package')
const size = ref('middle')
const keyword = ref('')
const list = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    list.value = await listTenantPackage({ name: keyword.value || undefined })
  } finally {
    loading.value = false
  }
}
onMounted(load)

const menuTree = ref([])
const catalog = ref()
async function loadMenuTree() {
  if (!menuTree.value.length) [menuTree.value, catalog.value] = await Promise.all([tenantPackageMenuTree().then(translateMenus), tenantFeatures()])
}

const rules = {
  name: [{ required: true, whitespace: true, message: t('system.tenantPackage.inputName') }],
  menuIds: [{ validator: (_, v) => (v?.length ? Promise.resolve() : Promise.reject(t('system.tenantPackage.selectMenus'))) }]
}
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, name: '', status: 1, remark: '', menuIds: [] }),
  beforeOpen: loadMenuTree,
  fetch: (record) => getTenantPackage(record.id),
  create: addTenantPackage,
  update: updateTenantPackage,
  toPayload: ({ name, status, remark, menuIds }) => ({ name, status, remark, menuIds }),
  onSuccess: load
})
const { formRef, form } = drawer

const remove = useAction(deleteTenantPackage, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
</script>

<style scoped>
.search {
  width: 200px;
}
.picker {
  margin-bottom: 16px;
}
.tree-label {
  margin-bottom: 8px;
  font-weight: 500;
}
.sub-text {
  margin-left: 6px;
  color: var(--app-text-3);
  font-size: 12px;
  font-weight: 400;
}
</style>
