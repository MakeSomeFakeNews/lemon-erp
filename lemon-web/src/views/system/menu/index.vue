<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="filter" @reset="resetQuery">
        <a-form-item :label="$t('system.menu.name')" name="name">
          <a-input v-model:value="query.name" :placeholder="$t('system.menu.inputName')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select
            v-model:value="query.status"
            :placeholder="$t('common.placeholder.all')"
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
          <template #icon><PlusOutlined /></template>{{ $t('system.menu.addMenu') }}
        </a-button>
        <a-button @click="toggleExpand">
          <template #icon><NodeExpandOutlined v-if="!expandedKeys.length" /><NodeCollapseOutlined v-else /></template>
          {{ expandedKeys.length ? $t('system.menu.collapseAll') : $t('system.menu.expandAll') }}
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
            <DictTag v-if="record.type !== MenuType.F && record.visible === 0" type="sys_show_hide" :value="record.visible" />
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button
                v-if="record.type !== MenuType.F"
                v-perm="'sys:menu:add'"
                type="link"
                size="small"
                @click="openCreate(record)"
              >
                <template #icon><PlusOutlined /></template>{{ $t('common.action.add') }}
              </a-button>
              <a-button v-perm="'sys:menu:edit'" type="link" size="small" @click="drawer.openEdit(record)">
                <template #icon><EditOutlined /></template>{{ $t('common.action.edit') }}
              </a-button>
              <span v-perm="'sys:menu:remove'">
                <a-popconfirm :title="$t('system.menu.confirmDelete', { name: record.name })" @confirm="remove(record.id)">
                  <a-button type="link" size="small" danger :disabled="!!record.children?.length">
                    <template #icon><DeleteOutlined /></template>{{ $t('common.action.delete') }}
                  </a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" :create-title="$t('system.menu.addMenu')" :edit-title="$t('system.menu.editMenu')" width="min(680px, 100vw)">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="24">
            <a-form-item :label="$t('system.menu.parent')" name="parentId">
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
            <a-form-item :label="$t('system.menu.type')" name="type">
              <a-radio-group v-model:value="form.type" button-style="solid">
                <a-radio-button v-for="o in dict.sys_menu_type" :key="o.value" :value="o.value">
                  {{ o.label }}
                </a-radio-button>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('system.menu.name')" name="name">
              <a-input v-model:value="form.name" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('common.field.sort')" name="sort">
              <a-input-number v-model:value="form.sort" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <template v-if="form.type !== MenuType.F">
            <a-col :xs="24" :sm="12">
              <a-form-item :label="$t('system.menu.icon')" name="icon">
                <IconPicker v-model="form.icon" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12">
              <a-form-item name="path">
                <template #label>
                  <a-tooltip :title="$t('system.menu.pathTip')">
                    {{ $t('system.menu.path') }} <QuestionCircleOutlined />
                  </a-tooltip>
                </template>
                <a-input v-model:value="form.path" :placeholder="form.type === MenuType.M ? '/system' : 'user'" />
              </a-form-item>
            </a-col>
          </template>
          <a-col v-if="form.type === MenuType.C" :xs="24" :sm="12">
            <a-form-item name="component">
              <template #label>
                <a-tooltip :title="$t('system.menu.componentTip')">
                  {{ $t('system.menu.component') }} <QuestionCircleOutlined />
                </a-tooltip>
              </template>
              <a-input v-model:value="form.component" placeholder="system/user/index" />
            </a-form-item>
          </a-col>
          <a-col v-if="form.type !== MenuType.M" :xs="24" :sm="12">
            <a-form-item :label="$t('system.menu.perms')" name="perms">
              <a-input v-model:value="form.perms" placeholder="sys:user:list" />
            </a-form-item>
          </a-col>
          <a-col v-if="form.type !== MenuType.F" :xs="24" :sm="12">
            <a-form-item :label="$t('system.menu.visible')" name="visible">
              <a-radio-group v-model:value="form.visible" :options="toOptions(dict.sys_show_hide, true)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('common.field.status')" name="status">
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
import { useI18n } from 'vue-i18n'
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
import { useAction } from '@/composables/useAction'

import { MenuType } from '@/constants/enums'
const { t } = useI18n()
const dict = useDict('sys_menu_type', 'sys_common_status', 'sys_show_hide')

const columns = [
  { title: t('system.menu.name'), key: 'name', width: 220 },
  { title: t('common.field.type'), key: 'type', width: 90 },
  { title: t('system.menu.path'), dataIndex: 'path', ellipsis: true },
  { title: t('system.menu.component'), dataIndex: 'component', ellipsis: true },
  { title: t('system.menu.perms'), key: 'perms', width: 200 },
  { title: t('common.field.sort'), dataIndex: 'sort', width: 70 },
  { title: t('common.field.status'), key: 'status', width: 140 },
  { title: t('common.action.operation'), key: 'action', width: 230, fixed: 'right' }
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
  successMessage: t('system.menu.savedTip'),
  onSuccess: load
})
const { form, formRef } = drawer

/** 新增：在当前行下新增时，目录下默认新增菜单，菜单下默认新增按钮，排在最后 */
function openCreate(parent) {
  drawer.openCreate(
    parent
      ? { parentId: parent.id, type: parent.type === MenuType.M ? 'C' : 'F', sort: parent.children?.length ?? 0 }
      : { sort: list.value.length }
  )
}

// 上级菜单只能选目录或菜单；编辑时不能选择自己及下级菜单
const withoutButtons = (nodes) =>
  nodes.filter((n) => n.type !== MenuType.F).map((n) => ({ ...n, children: n.children && withoutButtons(n.children) }))
const parentOptions = computed(() => [
  {
    id: 0,
    name: t('system.menu.root'),
    children: mapTree(withoutButtons(list.value), (n, parent) => ({
      id: n.id,
      name: n.name,
      disabled: parent?.disabled || n.id === form.value.id
    }))
  }
])
// 模块下的二级目录只作为分组，可以不填路由地址
const rules = computed(() => ({
  name: [{ required: true, message: t('system.menu.inputName') }],
  type: [{ required: true }],
  path: [{ required: form.value.type === MenuType.C || !form.value.parentId, message: t('system.menu.inputPath') }],
  component: [{ required: true, message: t('system.menu.inputComponent') }]
}))

const remove = useAction(deleteMenu, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
</script>

<style scoped>
.menu-name {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
</style>
