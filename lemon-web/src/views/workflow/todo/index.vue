<template>
  <TaskListPage
    :fetcher="pageTodoTask"
    :columns="columns"
    storage-key="workflow-todo"
    :action-text="$t('workflow.todo.approve')"
    :action-icon="AuditOutlined"
  >
    <template #title="{ total }">
      <span class="toolbar-title">{{ $t('workflow.todo.title') }} <a-badge :count="total" :overflow-count="999" /></span>
    </template>
    <template #cell="{ column, record }">
      <a-tag v-if="column.key === 'taskName'" color="orange" :bordered="false">{{ record.taskName }}</a-tag>
    </template>
  </TaskListPage>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { AuditOutlined } from '@ant-design/icons-vue'
import TaskListPage from '@/components/workflow/TaskListPage.vue'
import { pageTodoTask } from '@/api/workflow/task'

const { t } = useI18n()

const columns = [
  { title: t('workflow.list.processName'), key: 'processName', dataIndex: 'processName', width: 200, ellipsis: true },
  { title: t('workflow.list.currentNode'), key: 'taskName', width: 140 },
  { title: t('workflow.list.initiator'), dataIndex: 'startUserName', width: 120 },
  { title: t('workflow.list.startTime'), dataIndex: 'startTime', width: 170 },
  { title: t('workflow.todo.arriveTime'), dataIndex: 'createTime', width: 170 },
  { title: t('common.action.operation'), key: 'action', width: 110, fixed: 'right' }
]
</script>

<style scoped>
.toolbar-title {
  align-self: center;
  font-weight: 500;
}
</style>
