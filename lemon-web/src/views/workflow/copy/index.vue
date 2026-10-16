<template>
  <TaskListPage :fetcher="pageCopy" :columns="columns" storage-key="workflow-copy" row-key="id" />
</template>

<script setup>
import TaskListPage from '@/components/workflow/TaskListPage.vue'
import { pageCopy } from '@/api/workflow/copy'

const columns = [
  { title: '流程名称', key: 'processName', dataIndex: 'processName', width: 200, ellipsis: true },
  { title: '抄送节点', dataIndex: 'nodeName', width: 140 },
  { title: '发起人', dataIndex: 'startUserName', width: 120 },
  { title: '流程状态', key: 'status', width: 100 },
  { title: '抄送时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 100, fixed: 'right' }
]
</script>
