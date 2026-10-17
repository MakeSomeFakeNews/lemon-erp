<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-button @click="openLogs()">
          <template #icon><FileTextOutlined /></template>{{ $t('monitor.job.logs') }}
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
            <a-tag :color="record.status === 1 ? 'green' : 'default'">{{ record.status === 1 ? $t('common.status.enabled') : $t('common.status.disabled') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'lastStatus'">
            <a-tag v-if="record.lastStatus" :color="record.lastStatus === JobRunStatus.SUCCESS ? 'green' : 'red'">
              {{ record.lastStatus === JobRunStatus.SUCCESS ? $t('monitor.job.success') : $t('monitor.job.fail') }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button v-perm="'monitor:job:run'" type="link" size="small" :loading="running === record.id" @click="run(record)">
              <template #icon><PlayCircleOutlined /></template>{{ $t('monitor.job.run') }}
            </a-button>
            <a-button v-perm="'monitor:job:edit'" type="link" size="small" @click="drawer.openEdit(record)">
              <template #icon><EditOutlined /></template>{{ $t('common.action.modify') }}
            </a-button>
            <a-button type="link" size="small" @click="openLogs(record)">
              <template #icon><FileTextOutlined /></template>{{ $t('monitor.job.log') }}
            </a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" :edit-title="$t('monitor.job.editTitle')">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item :label="$t('monitor.job.job')">
          <a-input :value="$t('monitor.job.jobLabel', { name: form.name, key: form.jobKey })" disabled />
        </a-form-item>
        <a-form-item :label="$t('monitor.job.cron')" name="cron" :extra="$t('monitor.job.cronTip')">
          <a-input v-model:value="form.cron" placeholder="0 0 8 * * *" />
        </a-form-item>
        <a-form-item :label="$t('monitor.job.params')" name="params" :extra="$t('monitor.job.paramsTip')">
          <a-input v-model:value="form.params" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-radio-group v-model:value="form.status">
            <a-radio :value="1">{{ $t('common.status.enabled') }}</a-radio>
            <a-radio :value="0">{{ $t('common.status.disabled') }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('common.field.remark')" name="remark">
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
            {{ record.triggerType === JobTrigger.MANUAL ? $t('monitor.job.manual') : $t('monitor.job.scheduled') }}
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === JobRunStatus.SUCCESS ? 'green' : 'red'">
              {{ record.status === JobRunStatus.SUCCESS ? $t('monitor.job.success') : $t('monitor.job.fail') }}
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
import { useI18n } from 'vue-i18n'
import { EditOutlined, FileTextOutlined, PlayCircleOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import { listJob, pageJobLog, runJob, updateJob } from '@/api/monitor/job'
import { useColumns } from '@/composables/useColumns'
import { useFormDrawer } from '@/composables/useFormDrawer'

import { JobRunStatus, JobTrigger } from '@/constants/enums'
const { t } = useI18n()
const columns = [
  { title: t('monitor.job.name'), dataIndex: 'name', width: 180 },
  { title: t('monitor.job.jobKey'), dataIndex: 'jobKey', width: 180 },
  { title: 'Cron', dataIndex: 'cron', width: 140 },
  { title: t('monitor.job.params'), dataIndex: 'params', width: 120, ellipsis: true },
  { title: t('common.field.status'), key: 'status', width: 90 },
  { title: t('monitor.job.lastRun'), dataIndex: 'lastRunTime', width: 170 },
  { title: t('monitor.job.result'), key: 'lastStatus', width: 90 },
  { title: t('monitor.job.lastMessage'), dataIndex: 'lastMessage', ellipsis: true },
  { title: t('common.action.operation'), key: 'action', width: 240, fixed: 'right' }
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
    if (log.status === JobRunStatus.SUCCESS) message.success(log.message || t('monitor.job.runSuccess'))
    else message.error(log.message || t('monitor.job.runFail'))
    load()
  } finally {
    running.value = null
  }
}

const rules = {
  cron: [{ required: true, message: t('monitor.job.inputCron') }],
  status: [{ required: true, message: t('monitor.job.selectStatus') }]
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
const logTitle = computed(() => (logJob.value ? t('monitor.job.logsTitle', { name: logJob.value.name }) : t('monitor.job.logs')))
const logs = reactive({ list: [], total: 0, current: 1, size: 10, loading: false })
const logPagination = computed(() => ({
  current: logs.current,
  pageSize: logs.size,
  total: logs.total,
  showSizeChanger: true,
  showTotal: (total) => t('common.table.total', { total })
}))
const logColumns = [
  { title: t('monitor.job.job'), dataIndex: 'jobKey', width: 170 },
  { title: t('monitor.job.trigger'), key: 'triggerType', width: 90 },
  { title: t('monitor.job.startTime'), dataIndex: 'startTime', width: 170 },
  { title: t('monitor.job.costMs'), dataIndex: 'costMs', width: 110, align: 'right' },
  { title: t('monitor.job.result'), key: 'status', width: 80 },
  { title: t('common.field.description'), dataIndex: 'message', ellipsis: true }
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
