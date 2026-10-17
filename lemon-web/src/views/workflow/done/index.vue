<template>
  <TaskListPage :fetcher="pageDoneTask" :columns="columns" storage-key="workflow-done" :scroll-x="1100">
    <template #cell="{ column, record }">
      <DictTag v-if="column.key === 'result'" type="wf_task_action" :value="record.action" />
    </template>
  </TaskListPage>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import TaskListPage from '@/components/workflow/TaskListPage.vue'
import { pageDoneTask } from '@/api/workflow/task'
import { useDict } from '@/composables/useDict'

const { t } = useI18n()

useDict('wf_task_action')

const columns = [
  { title: t('workflow.list.processName'), key: 'processName', dataIndex: 'processName', width: 200, ellipsis: true },
  { title: t('workflow.done.taskNode'), dataIndex: 'taskName', width: 140 },
  { title: t('workflow.done.result'), key: 'result', width: 110 },
  { title: t('workflow.done.processStatus'), key: 'status', width: 120 },
  { title: t('workflow.list.initiator'), dataIndex: 'startUserName', width: 120 },
  { title: t('workflow.list.startTime'), dataIndex: 'startTime', width: 170 },
  { title: t('workflow.done.handleTime'), dataIndex: 'endTime', width: 170 },
  { title: t('common.action.operation'), key: 'action', width: 110, fixed: 'right' }
]
</script>
