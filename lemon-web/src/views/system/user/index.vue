<template>
  <div class="workspace">
    <!-- 部门树：点击筛选该部门及下级部门的用户 -->
    <a-card v-show="showDept" :bordered="false" class="page-card workspace-side">
      <DeptTree v-model="query.deptId" :tree="deptList" :loading="deptLoading" @change="search" />
    </a-card>

    <div class="table-page workspace-main">
      <a-card v-show="showSearch" :bordered="false" class="page-card">
        <SearchForm :model="query" @search="search" @reset="resetQuery">
          <a-form-item label="用户" name="username">
            <a-input v-model:value="query.username" placeholder="用户名或昵称" allow-clear />
          </a-form-item>
          <a-form-item label="手机号" name="phone">
            <a-input v-model:value="query.phone" placeholder="请输入手机号" allow-clear />
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
          :exporter="exportAll"
          v-model:size="size"
          v-model:checked="checked"
          v-model:show-search="showSearch"
          :columns="columns"
          @refresh="load"
        >
          <a-button v-perm="'sys:user:add'" type="primary" @click="openCreate">
            <template #icon><PlusOutlined /></template>新增用户
          </a-button>
          <a-button v-perm="'sys:user:edit'" :disabled="!selectedKeys.length" @click="openMove(selectedKeys)">
            <template #icon><SwapOutlined /></template>调整部门
          </a-button>
          <span v-perm="'sys:user:remove'">
            <a-popconfirm
              :title="`确定删除选中的 ${selectedKeys.length} 个用户？`"
              :disabled="!selectedKeys.length"
              @confirm="remove(selectedKeys)"
            >
              <a-button danger :disabled="!selectedKeys.length">
                <template #icon><DeleteOutlined /></template>批量删除
              </a-button>
            </a-popconfirm>
          </span>
          <span v-if="selectedKeys.length" class="selected-tip">
            已选 {{ selectedKeys.length }} 项 <a @click="selectedKeys = []">清空</a>
          </span>
          <template #extra>
            <a-tooltip :title="showDept ? '隐藏部门' : '显示部门'">
              <a-button type="text" @click="showDept = !showDept">
                <template #icon><MenuFoldOutlined v-if="showDept" /><MenuUnfoldOutlined v-else /></template>
              </a-button>
            </a-tooltip>
          </template>
        </TableToolbar>

        <a-table
          row-key="id"
          :size="size"
          :columns="visibleColumns"
          :data-source="list"
          :loading="loading"
          :pagination="pagination"
          :row-selection="rowSelection"
          :scroll="{ x: 1020, y: '100%' }"
          @change="onTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'username'">
              <UserCell :user="record" />
            </template>
            <template v-else-if="column.key === 'dept'">
              <a-tooltip v-if="record.deptId" :title="namePath(deptList, record.deptId)">
                <span>{{ record.deptName || '-' }}</span>
              </a-tooltip>
              <span v-else class="sub-text">未分配</span>
            </template>
            <template v-else-if="column.key === 'status'">
              <StatusSwitch
                :value="record.status"
                :name="record.username"
                :api="(s) => changeUserStatus(record.id, s)"
                :disabled="!canEdit || isProtected(record)"
                @changed="(s) => (record.status = s)"
              />
            </template>
            <template v-else-if="column.key === 'action'">
              <span class="table-actions">
                <a-button v-perm="'sys:user:edit'" type="link" size="small" @click="userForm.openEdit(record)">
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
    </div>

    <!-- 新增 / 编辑、调整部门 -->
    <UserFormDrawer ref="userForm" :dept-tree="deptList" @success="load" />
    <DeptMoveModal
      v-model:open="moveOpen"
      :user-ids="moveUserIds"
      :dept-tree="deptList"
      :default-dept-id="query.deptId"
      @success="onMoved"
    />

    <!-- 重置密码 -->
    <a-modal
      v-model:open="resetOpen"
      :title="`重置密码：${resetTarget?.username}`"
      :confirm-loading="resetting"
      destroy-on-close
      @ok="submitReset"
    >
      <a-form ref="resetRef" :model="resetForm" layout="vertical">
        <a-form-item label="新密码" name="password" :rules="passwordRules('请输入新密码')">
          <a-input-group compact class="password-group">
            <a-input-password v-model:value="resetForm.password" :placeholder="passwordHint()" autocomplete="new-password" />
            <a-tooltip title="随机生成并复制">
              <a-button @click="fillResetPassword">
                <template #icon><ThunderboltOutlined /></template>
              </a-button>
            </a-tooltip>
          </a-input-group>
        </a-form-item>
      </a-form>
      <a-alert type="warning" show-icon message="重置后该用户的所有登录会话将失效，需要使用新密码重新登录。" />
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Modal, message } from 'ant-design-vue'
import {
  DeleteOutlined,
  DownOutlined,
  EditOutlined,
  KeyOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  PlusOutlined,
  SwapOutlined,
  ThunderboltOutlined
} from '@ant-design/icons-vue'
import SearchForm from '@/components/SearchForm.vue'
import StatusSwitch from '@/components/StatusSwitch.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import DeptTree from '@/components/tree/DeptTree.vue'
import DeptMoveModal from '@/components/user/DeptMoveModal.vue'
import UserCell from '@/components/user/UserCell.vue'
import UserFormDrawer from '@/components/user/UserFormDrawer.vue'
import { changeUserStatus, deleteUser, pageUser, resetUserPassword } from '@/api/system/user'
import { deptTree } from '@/api/system/dept'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { SUPER_ADMIN_ID } from '@/utils/constants'
import { toOptions } from '@/utils/dict'
import { generateAndCopyPassword, passwordHint, passwordRules } from '@/utils/password'
import { load as loadPref, save as savePref } from '@/utils/storage'
import { namePath } from '@/utils/tree'

const userStore = useUserStore()
const canEdit = computed(() => userStore.hasPerm('sys:user:edit'))
const dict = useDict('sys_common_status')

const columns = [
  { title: '用户', key: 'username', width: 180 },
  { title: '部门', key: 'dept', width: 120, ellipsis: true },
  { title: '手机号', dataIndex: 'phone', width: 130 },
  { title: '邮箱', dataIndex: 'email', width: 180, ellipsis: true },
  { title: '状态', key: 'status', width: 90 },
  { title: '创建时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 150, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'system-user')
const showSearch = ref(true)
const showDept = ref(loadPref('user-page:show-dept', true))
watch(showDept, (v) => savePref('user-page:show-dept', v))

const query = reactive({ username: undefined, phone: undefined, status: undefined, deptId: undefined })
const { list, loading, size, pagination, selectedKeys, rowSelection, load, search, onTableChange, exportAll } = useTable(
  pageUser,
  query
)

function resetQuery() {
  Object.assign(query, { username: undefined, phone: undefined, status: undefined, deptId: undefined })
  search()
}

// 部门
const deptList = ref([])
const deptLoading = ref(false)

async function loadDepts() {
  deptLoading.value = true
  try {
    deptList.value = await deptTree()
  } finally {
    deptLoading.value = false
  }
}

onMounted(() => {
  load()
  loadDepts()
})

// 受保护的账号：内置超级管理员和当前登录用户不能删除 / 停用
const isProtected = (record) => record.id === SUPER_ADMIN_ID || record.id === userStore.user?.id

function moreActions(record) {
  const actions = []
  if (userStore.hasPerm('sys:user:edit')) actions.push({ key: 'move', label: '调整部门', icon: SwapOutlined })
  if (userStore.hasPerm('sys:user:resetPwd')) actions.push({ key: 'resetPwd', label: '重置密码', icon: KeyOutlined })
  if (userStore.hasPerm('sys:user:remove')) {
    actions.push({ key: 'remove', label: '删除', icon: DeleteOutlined, danger: true, disabled: isProtected(record) })
  }
  return actions
}

function onMoreAction(key, record) {
  if (key === 'move') openMove([record.id])
  if (key === 'resetPwd') openReset(record)
  if (key === 'remove') {
    Modal.confirm({
      title: `确定删除用户「${record.username}」？`,
      content: '删除后该用户的所有登录会话将立即失效。',
      okType: 'danger',
      onOk: () => remove([record.id])
    })
  }
}

async function remove(ids) {
  await deleteUser(ids)
  message.success('删除成功')
  selectedKeys.value = selectedKeys.value.filter((k) => !ids.includes(k))
  load()
}

// 新增 / 编辑
const userForm = ref()
/** 新增时默认选中左侧当前部门 */
const openCreate = () => userForm.value.openCreate({ deptId: query.deptId })

// 调整部门：工具栏为选中的用户，行内为单个用户
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

// 重置密码
const resetOpen = ref(false)
const resetting = ref(false)
const resetTarget = ref()
const resetRef = ref()
const resetForm = reactive({ password: '' })

function openReset(record) {
  resetTarget.value = record
  resetForm.password = ''
  resetOpen.value = true
}

async function fillResetPassword() {
  resetForm.password = await generateAndCopyPassword()
}

async function submitReset() {
  await resetRef.value.validate()
  resetting.value = true
  try {
    await resetUserPassword(resetTarget.value.id, resetForm.password)
    message.success('密码已重置')
    resetOpen.value = false
  } finally {
    resetting.value = false
  }
}
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.selected-tip {
  align-self: center;
  color: var(--app-text-2);
}
.password-group {
  display: flex;
}
.password-group :deep(.ant-input-affix-wrapper) {
  flex: 1;
}
</style>
