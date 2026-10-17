<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('hr.leave.apply')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('hr.leave.keywordPh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('hr.leave.kind')" name="kind">
          <a-select v-model:value="query.kind" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.hr_leave_kind)" />
        </a-form-item>
        <a-form-item :label="$t('hr.leave.leaveType')" name="type">
          <a-select v-model:value="query.type" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.hr_leave_type)" />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.hr_leave_status)" />
        </a-form-item>
        <a-form-item :label="$t('common.field.beginDate')" name="range">
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
          :message="$t('hr.leave.tip')"
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
            <DictTag v-if="record.type" :type="record.kind === LeaveKind.OVERTIME ? 'hr_overtime_comp' : 'hr_leave_type'" :value="record.type" />
          </template>
          <template v-else-if="column.key === 'time'">
            <div>{{ fmt(record.startTime) }}</div>
            <div class="sub-text">{{ $t('hr.leave.to') }} {{ fmt(record.endTime) }}</div>
          </template>
          <template v-else-if="column.key === 'hours'">{{ hoursText(record.hours) }}</template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="hr_leave_status" :value="record.status" />
            <div v-if="record.status === LeaveStatus.APPROVING && record.flowInstanceId" class="sub-text">{{ $t('hr.leave.inFlow') }}</div>
          </template>
          <template v-else-if="column.key === 'approve'">
            <template v-if="record.approveTime">
              <div>{{ record.approveByName || $t('hr.leave.flow') }} {{ fmt(record.approveTime) }}</div>
              <div v-if="record.approveComment" class="sub-text">{{ record.approveComment }}</div>
            </template>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button
              v-if="record.status === LeaveStatus.APPROVING && !record.flowInstanceId"
              v-perm="'hr:leave:approve'"
              type="link"
              size="small"
              @click="openApprove(record)"
            >
              {{ $t('hr.leave.approve') }}
            </a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="approveOpen" :title="$t('hr.leave.approveTitle', { name: current?.employeeName || '', no: current?.leaveNo || '' })" :confirm-loading="saving" destroy-on-close @ok="submit">
      <a-descriptions v-if="current" size="small" :column="1" bordered class="mb">
        <a-descriptions-item :label="$t('hr.leave.kind')">
          {{ dictLabel(dict.hr_leave_kind, current.kind) }}
          {{ dictLabel(current.kind === LeaveKind.OVERTIME ? dict.hr_overtime_comp : dict.hr_leave_type, current.type) }}
        </a-descriptions-item>
        <a-descriptions-item :label="$t('hr.leave.time')">{{ fmt(current.startTime) }} {{ $t('hr.leave.to') }} {{ fmt(current.endTime) }}</a-descriptions-item>
        <a-descriptions-item :label="$t('hr.leave.hours')">{{ hoursText(current.hours) }}</a-descriptions-item>
        <a-descriptions-item :label="$t('hr.leave.reason')">{{ current.reason || '-' }}</a-descriptions-item>
      </a-descriptions>
      <a-radio-group v-model:value="approved" class="mb">
        <a-radio-button :value="true">{{ $t('common.action.approve') }}</a-radio-button>
        <a-radio-button :value="false">{{ $t('common.action.reject') }}</a-radio-button>
      </a-radio-group>
      <a-textarea v-model:value="comment" :rows="2" :maxlength="255" :placeholder="$t('hr.leave.commentPh')" />
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import { hoursText } from '@/components/hr/hr'
import { approveLeave, pageLeave } from '@/api/hr'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { findDict, toOptions } from '@/utils/dict'

import { LeaveKind, LeaveStatus } from '@/constants/enums'
const { t } = useI18n()
const dict = useDict('hr_leave_kind', 'hr_leave_type', 'hr_leave_status', 'hr_overtime_comp')
const dictLabel = (list, v) => findDict(list, v)?.label || ''
const fmt = (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '')
const showSearch = ref(true)

const columns = [
  { title: t('hr.leave.no'), dataIndex: 'leaveNo', width: 150 },
  { title: t('hr.leave.employee'), key: 'employee', width: 170 },
  { title: t('hr.leave.kind'), key: 'kind', width: 140, dict: 'hr_leave_kind' },
  { title: t('hr.leave.time'), key: 'time', width: 170 },
  { title: t('hr.leave.hours'), key: 'hours', width: 140 },
  { title: t('hr.leave.reason'), dataIndex: 'reason', ellipsis: true },
  { title: t('common.field.status'), key: 'status', width: 100, dict: 'hr_leave_status' },
  { title: t('hr.leave.approve'), key: 'approve', width: 200 },
  { title: t('common.action.operation'), key: 'action', width: 80, fixed: 'right' }
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
    message.success(approved.value ? t('hr.leave.approved') : t('hr.leave.rejected'))
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
