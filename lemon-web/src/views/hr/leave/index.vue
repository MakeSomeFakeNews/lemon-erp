<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item label="申请" name="keyword">
          <a-input v-model:value="query.keyword" placeholder="单号、工号或姓名" allow-clear />
        </a-form-item>
        <a-form-item label="种类" name="kind">
          <a-select v-model:value="query.kind" placeholder="全部" allow-clear :options="toOptions(dict.hr_leave_kind)" />
        </a-form-item>
        <a-form-item label="假期类型" name="type">
          <a-select v-model:value="query.type" placeholder="全部" allow-clear :options="toOptions(dict.hr_leave_type)" />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear :options="toOptions(dict.hr_leave_status)" />
        </a-form-item>
        <a-form-item label="开始日期" name="range">
          <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <a-alert
          type="info"
          show-icon
          class="tip"
          message="员工在「员工自助」中提交请假、加班；在流程中心发布了 hr_leave 流程时走审批流程，否则在这里由人事审批。"
        />
      </TableToolbar>
      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1300, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'employee'">
            <div>{{ record.employeeName }}</div>
            <div class="sub-text">{{ record.empNo }} · {{ record.deptName || '-' }}</div>
          </template>
          <template v-else-if="column.key === 'kind'">
            <DictTag type="hr_leave_kind" :value="record.kind" />
            <DictTag v-if="record.type" :type="record.kind === 'OVERTIME' ? 'hr_overtime_comp' : 'hr_leave_type'" :value="record.type" />
          </template>
          <template v-else-if="column.key === 'time'">
            <div>{{ fmt(record.startTime) }}</div>
            <div class="sub-text">至 {{ fmt(record.endTime) }}</div>
          </template>
          <template v-else-if="column.key === 'hours'">{{ hoursText(record.hours) }}</template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="hr_leave_status" :value="record.status" />
            <div v-if="record.status === 'APPROVING' && record.flowInstanceId" class="sub-text">审批流程中</div>
          </template>
          <template v-else-if="column.key === 'approve'">
            <template v-if="record.approveTime">
              <div>{{ record.approveByName || '流程' }} {{ fmt(record.approveTime) }}</div>
              <div v-if="record.approveComment" class="sub-text">{{ record.approveComment }}</div>
            </template>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button
              v-if="record.status === 'APPROVING' && !record.flowInstanceId"
              v-perm="'hr:leave:approve'"
              type="link"
              size="small"
              @click="openApprove(record)"
            >
              审批
            </a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="approveOpen" :title="`审批：${current?.employeeName || ''} ${current?.leaveNo || ''}`" :confirm-loading="saving" destroy-on-close @ok="submit">
      <a-descriptions v-if="current" size="small" :column="1" bordered class="mb">
        <a-descriptions-item label="种类">
          {{ dictLabel(dict.hr_leave_kind, current.kind) }}
          {{ dictLabel(current.kind === 'OVERTIME' ? dict.hr_overtime_comp : dict.hr_leave_type, current.type) }}
        </a-descriptions-item>
        <a-descriptions-item label="时间">{{ fmt(current.startTime) }} 至 {{ fmt(current.endTime) }}</a-descriptions-item>
        <a-descriptions-item label="时长">{{ hoursText(current.hours) }}</a-descriptions-item>
        <a-descriptions-item label="事由">{{ current.reason || '-' }}</a-descriptions-item>
      </a-descriptions>
      <a-radio-group v-model:value="approved" class="mb">
        <a-radio-button :value="true">同意</a-radio-button>
        <a-radio-button :value="false">拒绝</a-radio-button>
      </a-radio-group>
      <a-textarea v-model:value="comment" :rows="2" :maxlength="255" placeholder="审批意见（选填）" />
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import { hoursText } from '@/components/hr/hr'
import { approveLeave, pageLeave } from '@/api/hr'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { findDict, toOptions } from '@/utils/dict'

const dict = useDict('hr_leave_kind', 'hr_leave_type', 'hr_leave_status', 'hr_overtime_comp')
const dictLabel = (list, v) => findDict(list, v)?.label || ''
const fmt = (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '')
const showSearch = ref(true)

const columns = [
  { title: '单号', dataIndex: 'leaveNo', width: 150 },
  { title: '员工', key: 'employee', width: 170 },
  { title: '种类', key: 'kind', width: 140, dict: 'hr_leave_kind' },
  { title: '时间', key: 'time', width: 170 },
  { title: '时长', key: 'hours', width: 140 },
  { title: '事由', dataIndex: 'reason', ellipsis: true },
  { title: '状态', key: 'status', width: 100, dict: 'hr_leave_status' },
  { title: '审批', key: 'approve', width: 200 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'hr-leave')

const emptyQuery = () => ({ keyword: undefined, kind: undefined, type: undefined, status: undefined, range: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageLeave, query, {
  params: ({ range, ...q }) => ({ ...q, beginDate: range?.[0], endDate: range?.[1] })
})
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}
onMounted(load)

const approveOpen = ref(false)
const saving = ref(false)
const current = ref()
const approved = ref(true)
const comment = ref('')
function openApprove(record) {
  current.value = record
  approved.value = true
  comment.value = ''
  approveOpen.value = true
}
async function submit() {
  saving.value = true
  try {
    await approveLeave(current.value.id, { approved: approved.value, comment: comment.value || null })
    message.success(approved.value ? '已同意' : '已拒绝')
    approveOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.mb {
  margin-bottom: 12px;
}
.tip {
  padding: 4px 12px;
}
</style>
