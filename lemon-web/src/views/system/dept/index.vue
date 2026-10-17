<template>
  <div class="workspace">
    <!-- 左侧：组织架构 -->
    <a-card :bordered="false" class="page-card workspace-side">
      <div class="workspace-side-head">
        <span>{{ $t('system.dept.orgChart') }}</span>
        <a-button v-perm="'sys:dept:add'" type="link" size="small" @click="openDeptCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('common.action.add') }}
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
                <template #icon><PlusOutlined /></template>{{ $t('system.dept.addChild') }}
              </a-button>
              <a-button v-perm="'sys:dept:edit'" @click="deptForm.openEdit(current)">
                <template #icon><EditOutlined /></template>{{ $t('common.action.edit') }}
              </a-button>
              <a-button v-perm="'sys:dept:remove'" danger @click="confirmRemoveDept(current)">
                <template #icon><DeleteOutlined /></template>{{ $t('common.action.delete') }}
              </a-button>
            </a-space>
          </div>
          <a-descriptions size="small" :column="{ xs: 1, sm: 2, xl: 4 }" class="dept-desc">
            <a-descriptions-item :label="$t('system.dept.leader')">{{ current.leaderName || '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('system.dept.contactPhone')">{{ current.phone || '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('common.field.email')">{{ current.email || '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('system.dept.children')">{{ current.children?.length || 0 }}</a-descriptions-item>
          </a-descriptions>
        </a-card>

        <a-card :bordered="false" class="page-card table-card">
          <TableToolbar
            :exporter="exportAll" v-model:size="size" @refresh="load">
            <a-button v-perm="'sys:user:add'" type="primary" @click="userForm.openCreate({ deptId: current.id })">
              <template #icon><UserAddOutlined /></template>{{ $t('system.dept.addMember') }}
            </a-button>
            <a-button v-perm="'sys:user:edit'" @click="pickerOpen = true">
              <template #icon><UsergroupAddOutlined /></template>{{ $t('system.dept.addExisting') }}
            </a-button>
            <a-button v-perm="'sys:user:edit'" :disabled="!selectedKeys.length" @click="openMove(selectedKeys)">
              <template #icon><SwapOutlined /></template>{{ $t('system.user.moveDept') }}{{ selectedKeys.length ? ` (${selectedKeys.length})` : '' }}
            </a-button>
            <template #extra>
              <a-input-search
                v-model:value="query.username"
                :placeholder="$t('system.dept.searchMember')"
                allow-clear
                class="member-search"
                @search="search"
              />
              <a-tooltip :title="$t('system.dept.includeChildrenTip')">
                <a-switch
                  v-model:checked="query.includeChildren"
                  :checked-children="$t('system.dept.includeChildren')"
                  :un-checked-children="$t('system.dept.selfOnly')"
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
                    <template #icon><EditOutlined /></template>{{ $t('common.action.edit') }}
                  </a-button>
                  <a-button v-perm="'sys:user:edit'" type="link" size="small" @click="openMove([record.id])">
                    <template #icon><SwapOutlined /></template>{{ $t('system.user.moveDept') }}
                  </a-button>
                </span>
              </template>
            </template>
            <template #emptyText>
              <a-empty :image="Empty.PRESENTED_IMAGE_SIMPLE" :description="$t('system.dept.noMembers')" />
            </template>
          </a-table>
        </a-card>
      </template>

      <a-card v-else :bordered="false" class="page-card empty-card">
        <a-empty :description="deptLoading ? $t('common.msg.loading') : $t('system.dept.empty')">
          <a-button v-if="!deptLoading" v-perm="'sys:dept:add'" type="primary" @click="openDeptCreate()">{{ $t('system.dept.addDept') }}</a-button>
        </a-empty>
      </a-card>
    </div>

    <!-- 部门表单 -->
    <DeptFormDrawer ref="deptForm" :dept-tree="deptList" @success="loadDepts" />

    <!-- 成员管理 -->
    <UserFormDrawer ref="userForm" :dept-tree="deptList" @success="load" />
    <UserPicker
      v-model:open="pickerOpen"
      :title="$t('system.dept.addMembersTitle', { name: current?.name })"
      :fetcher="pageUser"
      :disabled-row="(r) => r.deptId === current?.id"
      :disabled-text="$t('system.dept.alreadyIn')"
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
import { useI18n } from 'vue-i18n'
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

const { t } = useI18n()
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
    userStore.hasPerm('sys:dept:add') && { key: 'add', label: t('system.dept.addChild'), icon: PlusOutlined },
    userStore.hasPerm('sys:dept:edit') && { key: 'edit', label: t('common.action.edit'), icon: EditOutlined },
    userStore.hasPerm('sys:dept:remove') && { key: 'remove', label: t('common.action.delete'), icon: DeleteOutlined, danger: true }
  ].filter(Boolean)
)

function onNodeAction(key, node) {
  if (key === 'add') openDeptCreate(node)
  if (key === 'edit') deptForm.value.openEdit(node)
  if (key === 'remove') confirmRemoveDept(node)
}

function confirmRemoveDept(dept) {
  Modal.confirm({
    title: t('system.dept.confirmDelete', { name: dept.name }),
    content: t('system.dept.deleteTip'),
    okType: 'danger',
    async onOk() {
      const parentId = parentMap(deptList.value).get(dept.id)
      await deleteDept(dept.id)
      message.success(t('common.msg.deleteSuccess'))
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
  { title: t('common.field.user'), key: 'username', width: 200 },
  { title: t('common.field.dept'), key: 'dept', width: 140, ellipsis: true },
  { title: t('common.field.phone'), dataIndex: 'phone', width: 140 },
  { title: t('common.field.status'), key: 'status', width: 90 },
  { title: t('common.action.operation'), key: 'action', width: 230, fixed: 'right' }
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
  message.success(t('system.dept.addedN', { n: userIds.length }))
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
