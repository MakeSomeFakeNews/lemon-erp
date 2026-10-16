<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-button @click="openLogs()">
          <template #icon><FileTextOutlined /></template>执行日志
        </a-button>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: 1200, y: '100%' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'default'">{{ record.status === 1 ? '启用' : '停用' }}</a-tag>
          </template>
          <template v-else-if="column.key === 'lastStatus'">
            <a-tag v-if="record.lastStatus" :color="record.lastStatus === 'SUCCESS' ? 'green' : 'red'">
              {{ record.lastStatus === 'SUCCESS' ? '成功' : '失败' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button v-perm="'monitor:job:run'" type="link" size="small" :loading="running === record.id" @click="run(record)">
              <template #icon><PlayCircleOutlined /></template>执行
            </a-button>
            <a-button v-perm="'monitor:job:edit'" type="link" size="small" @click="drawer.openEdit(record)">
              <template #icon><EditOutlined /></template>修改
            </a-button>
            <a-button type="link" size="small" @click="openLogs(record)">
              <template #icon><FileTextOutlined /></template>日志
            </a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" edit-title="修改定时任务">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item label="任务">
          <a-input :value="`${form.name}（${form.jobKey}）`" disabled />
        </a-form-item>
        <a-form-item label="Cron 表达式" name="cron" extra="6 段：秒 分 时 日 月 周，如 0 30 1 * * * 表示每天 01:30">
          <a-input v-model:value="form.cron" placeholder="0 0 8 * * *" />
        </a-form-item>
        <a-form-item label="参数" name="params" extra="key=value，多个用英文逗号分隔，如 days=30">
          <a-input v-model:value="form.params" allow-clear />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-radio-group v-model:value="form.status">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">停用</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="备注" name="remark">
          <a-textarea v-model:value="form.remark" :rows="3" />
        </a-form-item>
      </a-form>
    </FormDrawer>

    <a-modal v-model:open="logOpen" :title="logTitle" :footer="null" width="min(960px, 100vw)">
      <a-table
        row-key="id"
        size="small"
        :columns="logColumns"
        :data-source="logs.list"
        :loading="logs.loading"
        :pagination="logPagination"
        :scroll="{ x: 800 }"
        @change="onLogChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'triggerType'">
            {{ record.triggerType === 'MANUAL' ? '手动' : '定时' }}
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 'SUCCESS' ? 'green' : 'red'">
              {{ record.status === 'SUCCESS' ? '成功' : '失败' }}
            </a-tag>
          </template>
        </template>
      </a-table>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { EditOutlined, FileTextOutlined, PlayCircleOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import { listJob, pageJobLog, runJob, updateJob } from '@/api/monitor/job'
import { useColumns } from '@/composables/useColumns'
import { useFormDrawer } from '@/composables/useFormDrawer'

const columns = [
  { title: '任务名称', dataIndex: 'name', width: 180 },
  { title: '任务标识', dataIndex: 'jobKey', width: 180 },
  { title: 'Cron', dataIndex: 'cron', width: 140 },
  { title: '参数', dataIndex: 'params', width: 120, ellipsis: true },
  { title: '状态', key: 'status', width: 80 },
  { title: '最近执行', dataIndex: 'lastRunTime', width: 170 },
  { title: '结果', key: 'lastStatus', width: 80 },
  { title: '执行说明', dataIndex: 'lastMessage', ellipsis: true },
  { title: '操作', key: 'action', width: 230, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns)
const size = ref('middle')
const list = ref([])
const loading = ref(false)
const running = ref(null)

async function load() {
  loading.value = true
  try {
    list.value = await listJob()
  } finally {
    loading.value = false
  }
}
onMounted(load)

async function run(record) {
  running.value = record.id
  try {
    const log = await runJob(record.id)
    if (log.status === 'SUCCESS') message.success(log.message || '执行成功')
    else message.error(log.message || '执行失败')
    load()
  } finally {
    running.value = null
  }
}

const rules = {
  cron: [{ required: true, message: '请填写 Cron 表达式' }],
  status: [{ required: true, message: '请选择状态' }]
}
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, jobKey: '', name: '', cron: '', params: '', status: 1, remark: '' }),
  create: () => Promise.resolve(),
  update: (id, form) => updateJob(id, form),
  toPayload: ({ cron, params, status, remark }) => ({ cron, params, status, remark }),
  onSuccess: load
})
const { formRef, form } = drawer

// 执行日志
const logOpen = ref(false)
const logJob = ref(null)
const logTitle = computed(() => (logJob.value ? `执行日志 - ${logJob.value.name}` : '执行日志'))
const logs = reactive({ list: [], total: 0, current: 1, size: 10, loading: false })
const logPagination = computed(() => ({
  current: logs.current,
  pageSize: logs.size,
  total: logs.total,
  showSizeChanger: true,
  showTotal: (t) => `共 ${t} 条`
}))
const logColumns = [
  { title: '任务', dataIndex: 'jobKey', width: 170 },
  { title: '触发', key: 'triggerType', width: 70 },
  { title: '开始时间', dataIndex: 'startTime', width: 170 },
  { title: '耗时(ms)', dataIndex: 'costMs', width: 90, align: 'right' },
  { title: '结果', key: 'status', width: 70 },
  { title: '说明', dataIndex: 'message', ellipsis: true }
]

async function loadLogs() {
  logs.loading = true
  try {
    const res = await pageJobLog({ current: logs.current, size: logs.size, jobKey: logJob.value?.jobKey })
    logs.list = res.records
    logs.total = res.total
  } finally {
    logs.loading = false
  }
}

function openLogs(record) {
  logJob.value = record || null
  logs.current = 1
  logOpen.value = true
  loadLogs()
}

function onLogChange(p) {
  logs.current = p.current
  logs.size = p.pageSize
  loadLogs()
}
</script>
