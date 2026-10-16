<template>
  <TaskListPage :fetcher="pageDoneTask" :columns="columns" storage-key="workflow-done" :scroll-x="1100">
    <template #cell="{ column, record }">
      <DictTag v-if="column.key === 'result'" type="wf_task_action" :value="record.action" />
    </template>
  </TaskListPage>
</template>

<script setup>
import DictTag from '@/components/DictTag.vue'
import TaskListPage from '@/components/workflow/TaskListPage.vue'
import { pageDoneTask } from '@/api/workflow/task'
import { useDict } from '@/composables/useDict'

useDict('wf_task_action')

const columns = [
  { title: '流程名称', key: 'processName', dataIndex: 'processName', width: 200, ellipsis: true },
  { title: '审批节点', dataIndex: 'taskName', width: 140 },
  { title: '处理结果', key: 'result', width: 100 },
  { title: '流程状态', key: 'status', width: 100 },
  { title: '发起人', dataIndex: 'startUserName', width: 120 },
  { title: '发起时间', dataIndex: 'startTime', width: 170 },
  { title: '处理时间', dataIndex: 'endTime', width: 170 },
  { title: '操作', key: 'action', width: 100, fixed: 'right' }
]
</script>
