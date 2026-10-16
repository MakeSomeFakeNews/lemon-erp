<template>
  <div class="workspace">
    <!-- 左侧：组织架构 -->
    <a-card :bordered="false" class="page-card workspace-side">
      <div class="workspace-side-head">
        <span>组织架构</span>
        <a-button v-perm="'sys:dept:add'" type="link" size="small" @click="openDeptCreate()">
          <template #icon><PlusOutlined /></template>新增
        </a-button>
      </div>
      <DeptTree v-model="currentId" :tree="deptList" :loading="deptLoading" :show-all="false">
        <template #actions="{ node }">
          <a-dropdown v-if="nodeActions.length" :trigger="['click']">
            <a-button type="text" size="small" class="node-more"><MoreOutlined /></a-button>
            <template #overlay>
              <a-menu @click="({ key }) => onNodeAction(key, node)">
                <a-menu-item v-for="a in nodeActions" :key="a.key" :danger="a.danger">
                  <component :is="a.icon" /> {{ a.label }}
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </template>
      </DeptTree>
    </a-card>

    <!-- 右侧：部门信息 + 成员 -->
    <div class="workspace-main">
      <template v-if="current">
        <a-card :bordered="false" class="page-card">
          <div class="dept-header">
            <div class="dept-heading">
              <div class="dept-title">
                <ApartmentOutlined />
                <span>{{ current.name }}</span>
                <DictTag type="sys_common_status" :value="current.status" />
              </div>
              <div class="dept-path">{{ namePath(deptList, current.id) }}</div>
            </div>
            <a-space wrap>
              <a-button v-perm="'sys:dept:add'" @click="openDeptCreate(current)">
                <template #icon><PlusOutlined /></template>新增下级
              </a-button>
              <a-button v-perm="'sys:dept:edit'" @click="deptForm.openEdit(current)">
                <template #icon><EditOutlined /></template>编辑
              </a-button>
              <a-button v-perm="'sys:dept:remove'" danger @click="confirmRemoveDept(current)">
                <template #icon><DeleteOutlined /></template>删除
              </a-button>
            </a-space>
          </div>
          <a-descriptions size="small" :column="{ xs: 1, sm: 2, xl: 4 }" class="dept-desc">
            <a-descriptions-item label="负责人">{{ current.leaderName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="联系电话">{{ current.phone || '-' }}</a-descriptions-item>
            <a-descriptions-item label="邮箱">{{ current.email || '-' }}</a-descriptions-item>
            <a-descriptions-item label="下级部门">{{ current.children?.length || 0 }} 个</a-descriptions-item>
          </a-descriptions>
        </a-card>

        <a-card :bordered="false" class="page-card table-card">
          <TableToolbar
            :exporter="exportAll" v-model:size="size" @refresh="load">
            <a-button v-perm="'sys:user:add'" type="primary" @click="userForm.openCreate({ deptId: current.id })">
              <template #icon><UserAddOutlined /></template>新增成员
            </a-button>
            <a-button v-perm="'sys:user:edit'" @click="pickerOpen = true">
              <template #icon><UsergroupAddOutlined /></template>添加已有用户
            </a-button>
            <a-button v-perm="'sys:user:edit'" :disabled="!selectedKeys.length" @click="openMove(selectedKeys)">
              <template #icon><SwapOutlined /></template>调整部门{{ selectedKeys.length ? `（${selectedKeys.length}）` : '' }}
            </a-button>
            <template #extra>
              <a-input-search
                v-model:value="query.username"
                placeholder="搜索成员"
                allow-clear
                class="member-search"
                @search="search"
              />
              <a-tooltip title="是否显示下级部门的成员">
                <a-switch
                  v-model:checked="query.includeChildren"
                  checked-children="含下级"
                  un-checked-children="仅本部门"
                  class="member-switch"
                  @change="search"
                />
              </a-tooltip>
            </template>
          </TableToolbar>

          <a-table
            row-key="id"
            :size="size"
            :columns="columns"
            :data-source="list"
            :loading="loading"
            :pagination="pagination"
            :row-selection="rowSelection"
            :scroll="{ x: 860, y: '100%' }"
            @change="onTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'username'">
                <UserCell :user="record" />
              </template>
              <template v-else-if="column.key === 'dept'">
                <span :class="{ 'sub-text': record.deptId !== current.id }">{{ record.deptName || '-' }}</span>
              </template>
              <template v-else-if="column.key === 'status'">
                <StatusSwitch
                  :value="record.status"
                  :name="record.username"
                  :api="(s) => changeUserStatus(record.id, s)"
                  :disabled="!canEditUser || record.id === SUPER_ADMIN_ID || record.id === userStore.user?.id"
                  @changed="(s) => (record.status = s)"
                />
              </template>
              <template v-else-if="column.key === 'action'">
                <span class="table-actions">
                  <a-button v-perm="'sys:user:edit'" type="link" size="small" @click="userForm.openEdit(record)">
                    <template #icon><EditOutlined /></template>编辑
                  </a-button>
                  <a-button v-perm="'sys:user:edit'" type="link" size="small" @click="openMove([record.id])">
                    <template #icon><SwapOutlined /></template>调整部门
                  </a-button>
                </span>
              </template>
            </template>
            <template #emptyText>
              <a-empty :image="Empty.PRESENTED_IMAGE_SIMPLE" description="该部门暂无成员" />
            </template>
          </a-table>
        </a-card>
      </template>

      <a-card v-else :bordered="false" class="page-card empty-card">
        <a-empty :description="deptLoading ? '加载中…' : '暂无可管理的部门'">
          <a-button v-if="!deptLoading" v-perm="'sys:dept:add'" type="primary" @click="openDeptCreate()">新增部门</a-button>
        </a-empty>
      </a-card>
    </div>

    <!-- 部门表单 -->
    <DeptFormDrawer ref="deptForm" :dept-tree="deptList" @success="loadDepts" />

    <!-- 成员管理 -->
    <UserFormDrawer ref="userForm" :dept-tree="deptList" @success="load" />
    <UserPicker
      v-model:open="pickerOpen"
      :title="`添加成员到「${current?.name}」`"
      :fetcher="pageUser"
      :disabled-row="(r) => r.deptId === current?.id"
      disabled-text="已在本部门"
      :submit="addMembers"
    />
    <DeptMoveModal
      v-model:open="moveOpen"
      :user-ids="moveUserIds"
      :dept-tree="deptList"
      @success="onMoved"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Empty, Modal, message } from 'ant-design-vue'
import {
  ApartmentOutlined,
  DeleteOutlined,
  EditOutlined,
  MoreOutlined,
  PlusOutlined,
  SwapOutlined,
  UserAddOutlined,
  UsergroupAddOutlined
} from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import StatusSwitch from '@/components/StatusSwitch.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import DeptTree from '@/components/tree/DeptTree.vue'
import DeptMoveModal from '@/components/user/DeptMoveModal.vue'
import UserCell from '@/components/user/UserCell.vue'
import UserFormDrawer from '@/components/user/UserFormDrawer.vue'
import UserPicker from '@/components/user/UserPicker.vue'
import DeptFormDrawer from './components/DeptFormDrawer.vue'
import { deleteDept, deptTree } from '@/api/system/dept'
import { changeUserDept, changeUserStatus, pageUser } from '@/api/system/user'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { SUPER_ADMIN_ID } from '@/utils/constants'
import { findNode, namePath, parentMap } from '@/utils/tree'

const userStore = useUserStore()
const canEditUser = computed(() => userStore.hasPerm('sys:user:edit'))
useDict('sys_common_status')

// ---------- 部门树 ----------
const deptList = ref([])
const deptLoading = ref(false)
const currentId = ref()
const current = computed(() => (currentId.value == null ? null : findNode(deptList.value, currentId.value)))

async function loadDepts() {
  deptLoading.value = true
  try {
    deptList.value = await deptTree()
    // 当前部门不存在（首次进入或已删除）时选中第一个部门
    if (!current.value) currentId.value = deptList.value[0]?.id
  } finally {
    deptLoading.value = false
  }
}

const nodeActions = computed(() =>
  [
    userStore.hasPerm('sys:dept:add') && { key: 'add', label: '新增下级', icon: PlusOutlined },
    userStore.hasPerm('sys:dept:edit') && { key: 'edit', label: '编辑', icon: EditOutlined },
    userStore.hasPerm('sys:dept:remove') && { key: 'remove', label: '删除', icon: DeleteOutlined, danger: true }
  ].filter(Boolean)
)

function onNodeAction(key, node) {
  if (key === 'add') openDeptCreate(node)
  if (key === 'edit') deptForm.value.openEdit(node)
  if (key === 'remove') confirmRemoveDept(node)
}

function confirmRemoveDept(dept) {
  Modal.confirm({
    title: `确定删除部门「${dept.name}」？`,
    content: '存在下级部门或成员时不能删除，请先调整。',
    okType: 'danger',
    async onOk() {
      const parentId = parentMap(deptList.value).get(dept.id)
      await deleteDept(dept.id)
      message.success('删除成功')
      if (currentId.value === dept.id) currentId.value = parentId ?? undefined
      await loadDepts()
    }
  })
}

// ---------- 部门表单 ----------
const deptForm = ref()
const openDeptCreate = (parent) => deptForm.value.openCreate(parent)

// ---------- 成员 ----------
const columns = [
  { title: '用户', key: 'username', width: 200 },
  { title: '部门', key: 'dept', width: 140, ellipsis: true },
  { title: '手机号', dataIndex: 'phone', width: 140 },
  { title: '状态', key: 'status', width: 90 },
  { title: '操作', key: 'action', width: 190, fixed: 'right' }
]
const query = reactive({ username: undefined, deptId: undefined, includeChildren: true })
const { list, loading, size, pagination, selectedKeys, rowSelection, load, search, onTableChange, exportAll } = useTable(
  pageUser,
  query
)

watch(currentId, (id) => {
  query.deptId = id
  query.username = undefined
  selectedKeys.value = []
  if (id != null) search()
})

const userForm = ref()
const pickerOpen = ref(false)

async function addMembers(userIds) {
  await changeUserDept(userIds, current.value.id)
  message.success(`已添加 ${userIds.length} 名成员`)
  load()
}

const moveOpen = ref(false)
const moveUserIds = ref([])
function openMove(ids) {
  moveUserIds.value = [...ids]
  moveOpen.value = true
}
function onMoved() {
  selectedKeys.value = []
  load()
}

onMounted(loadDepts)
</script>

<style scoped>
.node-more {
  height: 22px;
  padding: 0 4px;
}
.dept-header {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}
.dept-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
}
.dept-path {
  margin-top: 4px;
  color: var(--app-text-3);
  font-size: 12px;
}
.dept-desc {
  margin-top: 16px;
}
.member-search {
  width: 200px;
  margin-right: 8px;
}
.member-switch {
  margin-right: 8px;
}
.sub-text {
  color: var(--app-text-3);
}
.empty-card {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
