<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="filter" @reset="resetQuery">
        <a-form-item label="菜单名称" name="name">
          <a-input v-model:value="query.name" placeholder="请输入菜单名称" allow-clear />
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
        v-model:size="size"
        v-model:checked="checked"
        v-model:show-search="showSearch"
        :columns="columns"
        @refresh="load"
      >
        <a-button v-perm="'sys:menu:add'" type="primary" @click="openCreate()">
          <template #icon><PlusOutlined /></template>新增菜单
        </a-button>
        <a-button @click="toggleExpand">
          <template #icon><NodeExpandOutlined v-if="!expandedKeys.length" /><NodeCollapseOutlined v-else /></template>
          {{ expandedKeys.length ? '折叠全部' : '展开全部' }}
        </a-button>
      </TableToolbar>

      <a-table
        v-model:expanded-row-keys="expandedKeys"
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="treeData"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: 1100, y: '100%' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <span class="menu-name">
              <MenuIcon v-if="record.icon" :name="record.icon" />
              <HighlightText :text="record.name" :keyword="applied.name || ''" />
            </span>
          </template>
          <template v-else-if="column.key === 'type'">
            <DictTag type="sys_menu_type" :value="record.type" />
          </template>
          <template v-else-if="column.key === 'perms'">
            <a-typography-text v-if="record.perms" code copyable>{{ record.perms }}</a-typography-text>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="sys_common_status" :value="record.status" />
            <DictTag v-if="record.type !== 'F' && record.visible === 0" type="sys_show_hide" :value="record.visible" />
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button
                v-if="record.type !== 'F'"
                v-perm="'sys:menu:add'"
                type="link"
                size="small"
                @click="openCreate(record)"
              >
                <template #icon><PlusOutlined /></template>新增
              </a-button>
              <a-button v-perm="'sys:menu:edit'" type="link" size="small" @click="drawer.openEdit(record)">
                <template #icon><EditOutlined /></template>编辑
              </a-button>
              <span v-perm="'sys:menu:remove'">
                <a-popconfirm :title="`确定删除菜单「${record.name}」？`" @confirm="remove(record.id)">
                  <a-button type="link" size="small" danger :disabled="!!record.children?.length">
                    <template #icon><DeleteOutlined /></template>删除
                  </a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" create-title="新增菜单" edit-title="编辑菜单" width="min(680px, 100vw)">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="24">
            <a-form-item label="上级菜单" name="parentId">
              <a-tree-select
                v-model:value="form.parentId"
                :tree-data="parentOptions"
                :field-names="{ label: 'name', value: 'id', children: 'children' }"
                tree-default-expand-all
                show-search
                tree-node-filter-prop="name"
              />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="菜单类型" name="type">
              <a-radio-group v-model:value="form.type" button-style="solid">
                <a-radio-button v-for="o in dict.sys_menu_type" :key="o.value" :value="o.value">
                  {{ o.label }}
                </a-radio-button>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="菜单名称" name="name">
              <a-input v-model:value="form.name" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="排序" name="sort">
              <a-input-number v-model:value="form.sort" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <template v-if="form.type !== 'F'">
            <a-col :xs="24" :sm="12">
              <a-form-item label="图标" name="icon">
                <IconPicker v-model="form.icon" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12">
              <a-form-item name="path">
                <template #label>
                  <a-tooltip title="一级目录以 / 开头；二级目录作为侧边栏分组，可不填；菜单可写相对路径，会拼接在上级目录之后">
                    路由地址 <QuestionCircleOutlined />
                  </a-tooltip>
                </template>
                <a-input v-model:value="form.path" :placeholder="form.type === 'M' ? '/system' : 'user'" />
              </a-form-item>
            </a-col>
          </template>
          <a-col v-if="form.type === 'C'" :xs="24" :sm="12">
            <a-form-item name="component">
              <template #label>
                <a-tooltip title="相对 src/views，不含 .vue">
                  组件路径 <QuestionCircleOutlined />
                </a-tooltip>
              </template>
              <a-input v-model:value="form.component" placeholder="system/user/index" />
            </a-form-item>
          </a-col>
          <a-col v-if="form.type !== 'M'" :xs="24" :sm="12">
            <a-form-item label="权限标识" name="perms">
              <a-input v-model:value="form.perms" placeholder="sys:user:list" />
            </a-form-item>
          </a-col>
          <a-col v-if="form.type !== 'F'" :xs="24" :sm="12">
            <a-form-item label="是否显示" name="visible">
              <a-radio-group v-model:value="form.visible" :options="toOptions(dict.sys_show_hide, true)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="状态" name="status">
              <a-radio-group v-model:value="form.status" :options="toOptions(dict.sys_common_status, true)" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </FormDrawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  DeleteOutlined,
  EditOutlined,
  NodeCollapseOutlined,
  NodeExpandOutlined,
  PlusOutlined,
  QuestionCircleOutlined
} from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import IconPicker from '@/components/IconPicker.vue'
import MenuIcon from '@/components/MenuIcon.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import HighlightText from '@/components/tree/HighlightText.vue'
import { addMenu, deleteMenu, menuTree, updateMenu } from '@/api/system/menu'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { toOptions } from '@/utils/dict'
import { allIds, filterTree, mapTree } from '@/utils/tree'

const dict = useDict('sys_menu_type', 'sys_common_status', 'sys_show_hide')

const columns = [
  { title: '菜单名称', key: 'name', width: 220 },
  { title: '类型', key: 'type', width: 80 },
  { title: '路由地址', dataIndex: 'path', ellipsis: true },
  { title: '组件路径', dataIndex: 'component', ellipsis: true },
  { title: '权限标识', key: 'perms', width: 200 },
  { title: '排序', dataIndex: 'sort', width: 70 },
  { title: '状态', key: 'status', width: 130 },
  { title: '操作', key: 'action', width: 220, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'system-menu')
const showSearch = ref(true)
const size = ref('middle')

const list = ref([])
const loading = ref(false)
const expandedKeys = ref([])

async function load() {
  loading.value = true
  try {
    list.value = await menuTree()
  } finally {
    loading.value = false
  }
}
onMounted(load)

// 前端过滤：命中的节点保留其祖先
const query = reactive({ name: undefined, status: undefined })
const applied = reactive({ name: undefined, status: undefined })

const treeData = computed(() =>
  applied.name || applied.status != null
    ? filterTree(
        list.value,
        (n) => (!applied.name || n.name.includes(applied.name)) && (applied.status == null || n.status === applied.status)
      )
    : list.value
)

function filter() {
  Object.assign(applied, query)
  expandedKeys.value = applied.name || applied.status != null ? allIds(treeData.value) : []
}
function resetQuery() {
  query.name = undefined
  query.status = undefined
  filter()
}
function toggleExpand() {
  expandedKeys.value = expandedKeys.value.length ? [] : allIds(treeData.value)
}

// 表单
const drawer = useFormDrawer({
  emptyForm: () => ({
    id: undefined,
    parentId: 0,
    type: 'M',
    name: '',
    path: '',
    component: '',
    perms: '',
    icon: '',
    sort: 0,
    visible: 1,
    status: 1
  }),
  create: addMenu,
  update: updateMenu,
  successMessage: '保存成功，刷新页面后左侧菜单生效',
  onSuccess: load
})
const { form, formRef } = drawer

/** 新增：在当前行下新增时，目录下默认新增菜单，菜单下默认新增按钮，排在最后 */
function openCreate(parent) {
  drawer.openCreate(
    parent
      ? { parentId: parent.id, type: parent.type === 'M' ? 'C' : 'F', sort: parent.children?.length ?? 0 }
      : { sort: list.value.length }
  )
}

// 上级菜单只能选目录或菜单；编辑时不能选择自己及下级菜单
const withoutButtons = (nodes) =>
  nodes.filter((n) => n.type !== 'F').map((n) => ({ ...n, children: n.children && withoutButtons(n.children) }))
const parentOptions = computed(() => [
  {
    id: 0,
    name: '顶级菜单',
    children: mapTree(withoutButtons(list.value), (n, parent) => ({
      id: n.id,
      name: n.name,
      disabled: parent?.disabled || n.id === form.value.id
    }))
  }
])
// 模块下的二级目录只作为分组，可以不填路由地址
const rules = computed(() => ({
  name: [{ required: true, message: '请输入菜单名称' }],
  type: [{ required: true }],
  path: [{ required: form.value.type === 'C' || !form.value.parentId, message: '请输入路由地址' }],
  component: [{ required: true, message: '请输入组件路径' }]
}))

async function remove(id) {
  await deleteMenu(id)
  message.success('删除成功')
  load()
}
</script>

<style scoped>
.menu-name {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
</style>
