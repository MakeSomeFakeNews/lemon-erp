<template>
  <TaskListPage :fetcher="pageCopy" :columns="columns" storage-key="workflow-copy" row-key="id" />
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import TaskListPage from '@/components/workflow/TaskListPage.vue'
import { actionColumn } from '@/components/common/tableActions'
import { pageCopy } from '@/api/workflow/copy'

const { t } = useI18n()

const columns = [
  { title: t('workflow.list.processName'), key: 'processName', dataIndex: 'processName', width: 200, ellipsis: true },
  { title: t('workflow.copy.node'), dataIndex: 'nodeName', width: 140 },
  { title: t('workflow.list.initiator'), dataIndex: 'startUserName', width: 120 },
  { title: t('workflow.done.processStatus'), key: 'status', width: 120 },
  { title: t('workflow.copy.time'), dataIndex: 'createTime', width: 170 },
  actionColumn(1, [t('common.action.view')])
]
</script>
