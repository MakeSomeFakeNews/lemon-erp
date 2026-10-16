<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-input-search v-model:value="keyword" placeholder="套餐名称" allow-clear class="search" @search="load" />
        <a-button v-perm="'sys:tenantPackage:add'" type="primary" @click="drawer.openCreate()">
          <template #icon><PlusOutlined /></template>新增套餐
        </a-button>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: 800, y: '100%' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'default'">{{ record.status === 1 ? '启用' : '停用' }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button v-perm="'sys:tenantPackage:edit'" type="link" size="small" @click="drawer.openEdit(record)">
                <template #icon><EditOutlined /></template>编辑
              </a-button>
              <span v-perm="'sys:tenantPackage:remove'">
                <a-popconfirm :title="`确定删除套餐「${record.name}」？`" @confirm="remove(record.id)">
                  <a-button type="link" danger size="small" :disabled="record.tenantCount > 0"><DeleteOutlined /></a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" create-title="新增套餐" edit-title="编辑套餐" width="min(760px, 100vw)">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item label="套餐名称" name="name"><a-input v-model:value="form.name" :maxlength="64" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="状态" name="status" extra="停用后不能用于新建租户，已有租户不受影响">
              <a-radio-group v-model:value="form.status">
                <a-radio :value="1">启用</a-radio>
                <a-radio :value="0">停用</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" name="remark"><a-textarea v-model:value="form.remark" :rows="2" :maxlength="255" /></a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item
              label="可用菜单"
              name="menuIds"
              extra="租户管理员拥有套餐内的全部菜单；修改后，使用该套餐的租户立即按新菜单生效。菜单、字典、租户、服务监控等平台功能不能加入套餐"
            >
              <!-- 树中的搜索框、勾选框不作为表单字段收集，校验仍按 menuIds -->
              <a-form-item-rest>
                <CheckTree v-model="form.menuIds" :tree="menuTree" check-ancestors height="calc(100vh - 460px)" />
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
import { message } from 'ant-design-vue'
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons-vue'
import CheckTree from '@/components/tree/CheckTree.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import {
  addTenantPackage,
  deleteTenantPackage,
  getTenantPackage,
  listTenantPackage,
  tenantPackageMenuTree,
  updateTenantPackage
} from '@/api/system/tenant'
import { useColumns } from '@/composables/useColumns'
import { useFormDrawer } from '@/composables/useFormDrawer'

const columns = [
  { title: '套餐名称', dataIndex: 'name', width: 200 },
  { title: '租户数', dataIndex: 'tenantCount', width: 90, align: 'right' },
  { title: '备注', dataIndex: 'remark', ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', width: 170 },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 140, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'system-tenant-package')
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
async function loadMenuTree() {
  if (!menuTree.value.length) menuTree.value = await tenantPackageMenuTree()
}

const rules = {
  name: [{ required: true, whitespace: true, message: '请输入套餐名称' }],
  menuIds: [{ validator: (_, v) => (v?.length ? Promise.resolve() : Promise.reject('请选择套餐包含的菜单')) }]
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

async function remove(id) {
  await deleteTenantPackage(id)
  message.success('删除成功')
  load()
}
</script>

<style scoped>
.search {
  width: 200px;
}
</style>
