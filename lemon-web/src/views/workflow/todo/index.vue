<template>
  <TaskListPage
    :fetcher="pageTodoTask"
    :columns="columns"
    storage-key="workflow-todo"
    action-text="审批"
    :action-icon="AuditOutlined"
  >
    <template #title="{ total }">
      <span class="toolbar-title">待我审批 <a-badge :count="total" :overflow-count="999" /></span>
    </template>
    <template #cell="{ column, record }">
      <a-tag v-if="column.key === 'taskName'" color="orange" :bordered="false">{{ record.taskName }}</a-tag>
    </template>
  </TaskListPage>
</template>

<script setup>
import { AuditOutlined } from '@ant-design/icons-vue'
import TaskListPage from '@/components/workflow/TaskListPage.vue'
import { pageTodoTask } from '@/api/workflow/task'

const columns = [
  { title: '流程名称', key: 'processName', dataIndex: 'processName', width: 200, ellipsis: true },
  { title: '当前节点', key: 'taskName', width: 140 },
  { title: '发起人', dataIndex: 'startUserName', width: 120 },
  { title: '发起时间', dataIndex: 'startTime', width: 170 },
  { title: '到达时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 100, fixed: 'right' }
]
</script>

<style scoped>
.toolbar-title {
  align-self: center;
  font-weight: 500;
}
</style>
