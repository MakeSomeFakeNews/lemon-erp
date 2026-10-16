<template>
  <div class="user-panel">
    <div class="user-panel-toolbar">
      <a-space wrap>
        <a-button v-if="canAssign" type="primary" @click="pickerOpen = true">
          <template #icon><UsergroupAddOutlined /></template>添加用户
        </a-button>
        <a-popconfirm
          v-if="canAssign"
          :title="`确定将选中的 ${selectedKeys.length} 个用户移出该角色？`"
          :disabled="!selectedKeys.length"
          @confirm="remove(selectedKeys)"
        >
          <a-button danger :disabled="!selectedKeys.length">
            <template #icon><UserDeleteOutlined /></template>批量移除
          </a-button>
        </a-popconfirm>
      </a-space>
      <a-input-search
        v-model:value="query.username"
        placeholder="搜索用户名或昵称"
        allow-clear
        class="user-panel-search"
        @search="search"
      />
    </div>
    <a-table
      row-key="id"
      size="middle"
      :columns="columns"
      :data-source="list"
      :loading="loading"
      :pagination="pagination"
      :row-selection="canAssign ? rowSelection : undefined"
      :scroll="{ x: 700 }"
      @change="onTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'username'">
          <UserCell :user="record" />
        </template>
        <template v-else-if="column.key === 'status'">
          <DictTag type="sys_common_status" :value="record.status" />
        </template>
        <template v-else-if="column.key === 'action'">
          <a-popconfirm :title="`确定将「${record.username}」移出该角色？`" @confirm="remove([record.id])">
            <a-button type="link" size="small" danger :disabled="isBuiltinAdmin(record)">
              <template #icon><UserDeleteOutlined /></template>移除
            </a-button>
          </a-popconfirm>
        </template>
      </template>
      <template #emptyText>
        <a-empty :image="Empty.PRESENTED_IMAGE_SIMPLE" description="还没有用户拥有该角色" />
      </template>
    </a-table>

    <UserPicker
      v-model:open="pickerOpen"
      :title="`为「${role.name}」添加用户`"
      :fetcher="(params) => pageRoleUsers(role.id, { ...params, assigned: false })"
      :submit="add"
    />
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { Empty, message } from 'ant-design-vue'
import { UserDeleteOutlined, UsergroupAddOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import UserCell from '@/components/user/UserCell.vue'
import UserPicker from '@/components/user/UserPicker.vue'
import { addRoleUsers, pageRoleUsers, removeRoleUsers } from '@/api/system/role'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { ADMIN_ROLE, SUPER_ADMIN_ID } from '@/utils/constants'

/** 角色成员：查看、添加、移除拥有该角色的用户 */
const props = defineProps({
  role: { type: Object, required: true },
  canAssign: Boolean
})
const emit = defineEmits(['changed'])
useDict('sys_common_status')

const columns = computed(() => [
  { title: '用户', key: 'username', width: 200 },
  { title: '部门', dataIndex: 'deptName', width: 140, ellipsis: true },
  { title: '手机号', dataIndex: 'phone', width: 140 },
  { title: '状态', key: 'status', width: 90 },
  ...(props.canAssign ? [{ title: '操作', key: 'action', width: 100, fixed: 'right' }] : [])
])

const query = reactive({ username: undefined })
const { list, loading, pagination, selectedKeys, rowSelection, search, load, onTableChange } = useTable(
  (params) => pageRoleUsers(props.role.id, { ...params, assigned: true }),
  query
)

watch(
  () => props.role.id,
  () => {
    query.username = undefined
    selectedKeys.value = []
    search()
  },
  { immediate: true }
)

// 内置超级管理员必须保留超级管理员角色
const isBuiltinAdmin = (user) => props.role.code === ADMIN_ROLE && user.id === SUPER_ADMIN_ID

const pickerOpen = ref(false)

async function add(userIds) {
  await addRoleUsers(props.role.id, userIds)
  message.success(`已添加 ${userIds.length} 个用户`)
  load()
  emit('changed')
}

async function remove(userIds) {
  await removeRoleUsers(props.role.id, userIds)
  message.success('已移除')
  selectedKeys.value = selectedKeys.value.filter((k) => !userIds.includes(k))
  load()
  emit('changed')
}
</script>

<style scoped>
.user-panel-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 12px;
}
.user-panel-search {
  width: 240px;
  max-width: 100%;
}
</style>
