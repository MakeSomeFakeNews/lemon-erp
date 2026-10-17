<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card">
      <div class="head">
        <a-segmented v-model:value="tab" :options="TABS" />
        <template v-if="tab === 'records'">
          <a-input v-model:value="query.keyword" :placeholder="$t('hr.attendance.keywordPh')" allow-clear class="w160" @press-enter="search" />
          <DeptSelect v-model:value="query.deptId" :placeholder="$t('hr.attendance.allDepts')" class="w180" @update:value="search" />
          <a-select v-model:value="query.status" :placeholder="$t('hr.attendance.allStatus')" allow-clear class="w120" :options="toOptions(dict.hr_attendance_status)" @change="search" />
          <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" @change="search" />
          <a-button type="primary" @click="search">{{ $t('common.action.query') }}</a-button>
        </template>
        <template v-else-if="tab === 'summary'">
          <PeriodPicker v-model:value="period" @update:value="loadSummary" />
          <DeptSelect v-model:value="summaryDept" :placeholder="$t('hr.attendance.allDepts')" class="w180" @update:value="loadSummary" />
          <a-input v-model:value="summaryKeyword" :placeholder="$t('hr.attendance.keywordPh')" allow-clear class="w160" @press-enter="loadSummary" />
          <a-button type="primary" @click="loadSummary">{{ $t('common.action.query') }}</a-button>
        </template>
        <template v-else>
          <span class="sub-text">{{ $t('hr.attendance.workdayTip') }}</span>
        </template>
      </div>
    </a-card>

    <!-- 考勤记录 -->
    <a-card v-show="tab === 'records'" :bordered="false" class="page-card table-card">
      <TableToolbar
        :exporter="exportAll" v-model:size="size" :columns="columns" @refresh="load">
        <a-button v-perm="'hr:attendance:edit'" type="primary" @click="openManual()">
          <template #icon><PlusOutlined /></template>{{ $t('hr.attendance.manualAdd') }}
        </a-button>
      </TableToolbar>
      <a-table
        row-key="id"
        :size="size"
        :columns="columns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1100, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'employee'">
            <div>{{ record.employeeName }}</div>
            <div class="sub-text">{{ record.empNo }} · {{ record.deptName || '-' }}</div>
          </template>
          <template v-else-if="column.key === 'workDate'">
            {{ record.workDate }} <span class="sub-text">{{ weekday(record.workDate) }}</span>
          </template>
          <template v-else-if="column.key === 'checkIn'">
            {{ timeText(record.checkIn) }}
            <span v-if="record.lateMinutes" class="warn">{{ $t('hr.attendance.late', { n: record.lateMinutes }) }}</span>
          </template>
          <template v-else-if="column.key === 'checkOut'">
            {{ timeText(record.checkOut) }}
            <span v-if="record.earlyMinutes" class="warn">{{ $t('hr.attendance.early', { n: record.earlyMinutes }) }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="hr_attendance_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'source'">
            <a-tag v-if="record.source === 'MANUAL'" color="purple" :bordered="false">{{ $t('hr.attendance.manual') }}</a-tag>
            <span v-else class="sub-text">{{ $t('hr.attendance.punch') }}</span>
            <div v-if="record.remark" class="sub-text">{{ record.remark }}</div>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button v-perm="'hr:attendance:edit'" type="link" size="small" @click="openManual(record)">{{ $t('hr.attendance.fix') }}</a-button>
              <span v-perm="'hr:attendance:edit'">
                <a-popconfirm :title="$t('hr.attendance.confirmDeleteRecord')" @confirm="removeRecord(record.id)">
                  <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 月度汇总 -->
    <a-card v-show="tab === 'summary'" :bordered="false" class="page-card table-card">
      <a-table
        row-key="employeeId"
        size="middle"
        :columns="summaryColumns"
        :data-source="summary"
        :loading="summaryLoading"
        :pagination="{ pageSize: 50, showTotal: (n) => $t('hr.attendance.totalPeople', { n }) }"
        :scroll="{ x: 1500, y: '100%' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'employee'">
            <div>{{ record.employeeName }}</div>
            <div class="sub-text">{{ record.empNo }} · {{ record.deptName || '-' }}</div>
          </template>
          <template v-else-if="column.key === 'absentDays'">
            <span :class="{ danger: record.absentDays > 0 }">{{ record.absentDays }}</span>
          </template>
          <template v-else-if="column.key?.startsWith('leave_')">
            {{ Number(record.leaveHours[column.key.slice(6)] || 0) || '' }}
          </template>
          <template v-else-if="column.key === 'overtime'">
            {{ Number(record.overtimeHours) || '' }}
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 班次与节假日 -->
    <a-row v-show="tab === 'config'" :gutter="16" class="config">
      <a-col :xs="24" :lg="11">
        <a-card :bordered="false" class="page-card" :title="$t('hr.attendance.shift')">
          <template #extra>
            <a-button v-perm="'hr:attendance:config'" size="small" type="primary" @click="openShift()">{{ $t('hr.attendance.addShift') }}</a-button>
          </template>
          <a-table row-key="id" size="small" :columns="shiftColumns" :data-source="shifts" :pagination="false">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'name'">
                {{ record.name }} <a-tag v-if="record.isDefault" color="blue" :bordered="false">{{ $t('hr.attendance.default') }}</a-tag>
              </template>
              <template v-else-if="column.key === 'time'">{{ record.startTime }} - {{ record.endTime }}</template>
              <template v-else-if="column.key === 'action'">
                <span v-if="userStore.hasPerm('hr:attendance:config')" class="table-actions">
                  <a-button type="link" size="small" @click="openShift(record)">{{ $t('common.action.edit') }}</a-button>
                  <a-popconfirm v-if="!record.isDefault" :title="$t('hr.attendance.confirmDeleteShift')" @confirm="removeShift(record.id)">
                    <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
                  </a-popconfirm>
                </span>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="13">
        <a-card :bordered="false" class="page-card" :title="$t('hr.attendance.holidays')">
          <template #extra>
            <a-space>
              <a-select v-model:value="holidayYear" :options="years" class="w100" @change="loadHolidays" />
              <a-button v-perm="'hr:attendance:config'" size="small" type="primary" @click="openHoliday()">{{ $t('common.action.add') }}</a-button>
            </a-space>
          </template>
          <a-table row-key="id" size="small" :columns="holidayColumns" :data-source="holidays" :pagination="{ pageSize: 15 }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'date'">{{ record.holidayDate }} <span class="sub-text">{{ weekday(record.holidayDate) }}</span></template>
              <template v-else-if="column.key === 'type'"><DictTag type="hr_holiday_type" :value="record.type" /></template>
              <template v-else-if="column.key === 'action'">
                <span v-if="userStore.hasPerm('hr:attendance:config')" class="table-actions">
                  <a-button type="link" size="small" @click="openHoliday(record)">{{ $t('common.action.edit') }}</a-button>
                  <a-popconfirm :title="$t('hr.attendance.confirmDeleteHoliday')" @confirm="removeHoliday(record.id)">
                    <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
                  </a-popconfirm>
                </span>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>

    <!-- 补录 -->
    <a-modal v-model:open="manualOpen" :title="$t('hr.attendance.manualAdd')" :confirm-loading="saving" destroy-on-close @ok="saveManual">
      <a-form ref="manualRef" :model="manualForm" layout="vertical">
        <a-form-item :label="$t('hr.attendance.employee')" name="employeeId" :rules="[{ required: true, message: $t('hr.attendance.rule.employee') }]">
          <EmployeeSelect v-model:value="manualForm.employeeId" :label="manualForm.employeeLabel" />
        </a-form-item>
        <a-form-item :label="$t('common.field.date')" name="workDate" :rules="[{ required: true, message: $t('hr.attendance.rule.date') }]">
          <a-date-picker v-model:value="manualForm.workDate" value-format="YYYY-MM-DD" :disabled-date="(d) => d.isAfter(dayjs(), 'day')" class="full" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="$t('hr.attendance.startTime')" name="checkIn">
              <a-time-picker v-model:value="manualForm.checkIn" format="HH:mm" value-format="HH:mm" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('hr.attendance.endTime')" name="checkOut">
              <a-time-picker v-model:value="manualForm.checkOut" format="HH:mm" value-format="HH:mm" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item :label="$t('hr.attendance.remarkLabel')" name="remark" :rules="[{ required: true, whitespace: true, message: $t('hr.attendance.rule.remark') }]">
          <a-input v-model:value="manualForm.remark" :maxlength="255" :placeholder="$t('hr.attendance.remarkPh')" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 班次 -->
    <a-modal v-model:open="shiftOpen" :title="shiftForm.id ? $t('hr.attendance.editShift') : $t('hr.attendance.addShift')" :confirm-loading="saving" destroy-on-close @ok="saveShift">
      <a-form ref="shiftRef" :model="shiftForm" layout="vertical">
        <a-form-item :label="$t('hr.attendance.shiftName')" name="name" :rules="[{ required: true, whitespace: true, message: $t('hr.attendance.rule.shiftName') }]">
          <a-input v-model:value="shiftForm.name" :maxlength="64" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="$t('hr.attendance.startTime')" name="startTime" :rules="[{ required: true, message: $t('hr.attendance.rule.startTime') }]">
              <a-time-picker v-model:value="shiftForm.startTime" format="HH:mm" value-format="HH:mm" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('hr.attendance.endTime')" name="endTime" :rules="[{ required: true, message: $t('hr.attendance.rule.endTime') }]">
              <a-time-picker v-model:value="shiftForm.endTime" format="HH:mm" value-format="HH:mm" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('hr.attendance.flex')" name="flexMinutes">
              <a-input-number v-model:value="shiftForm.flexMinutes" :min="0" :max="120" :addon-after="$t('hr.attendance.minutes')" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('hr.attendance.workHours')" name="workHours" :extra="$t('hr.attendance.workHoursExtra')">
              <a-input-number v-model:value="shiftForm.workHours" :min="1" :max="12" :step="0.5" :addon-after="$t('hr.attendance.hourUnit')" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item name="isDefault">
          <a-checkbox v-model:checked="shiftForm.isDefaultBool">{{ $t('hr.attendance.setDefault') }}</a-checkbox>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 节假日 -->
    <a-modal v-model:open="holidayOpen" :title="holidayForm.id ? $t('hr.attendance.editHoliday') : $t('hr.attendance.addHoliday')" :confirm-loading="saving" destroy-on-close @ok="saveHoliday">
      <a-form ref="holidayRef" :model="holidayForm" layout="vertical">
        <a-form-item :label="$t('common.field.date')" name="holidayDate" :rules="[{ required: true, message: $t('hr.attendance.rule.date') }]">
          <a-date-picker v-model:value="holidayForm.holidayDate" value-format="YYYY-MM-DD" class="full" />
        </a-form-item>
        <a-form-item :label="$t('common.field.type')" name="type" :rules="[{ required: true, message: $t('hr.attendance.rule.type') }]">
          <a-radio-group v-model:value="holidayForm.type" :options="toOptions(dict.hr_holiday_type)" />
        </a-form-item>
        <a-form-item :label="$t('common.field.name')" name="name" :rules="[{ required: true, whitespace: true, message: $t('hr.attendance.rule.name') }]">
          <a-input v-model:value="holidayForm.name" :maxlength="64" :placeholder="$t('hr.attendance.holidayPh')" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import DictTag from '@/components/DictTag.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import DeptSelect from '@/components/fin/DeptSelect.vue'
import PeriodPicker from '@/components/fin/PeriodPicker.vue'
import EmployeeSelect from '@/components/hr/EmployeeSelect.vue'
import { timeText } from '@/components/hr/hr'
import {
  addHoliday,
  addShift,
  attendanceSummary,
  deleteAttendance,
  deleteHoliday,
  deleteShift,
  listHoliday,
  listShift,
  pageAttendance,
  saveAttendance,
  updateHoliday,
  updateShift
} from '@/api/hr'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'

const { t } = useI18n()

const userStore = useUserStore()
const dict = useDict('hr_attendance_status', 'hr_leave_type', 'hr_holiday_type')
const TABS = [
  { label: t('hr.attendance.tab.records'), value: 'records' },
  { label: t('hr.attendance.tab.summary'), value: 'summary' },
  { label: t('hr.attendance.tab.config'), value: 'config' }
]
const tab = ref('records')
const weekday = (d) => t(`hr.attendance.week.${dayjs(d).day()}`)

// 考勤记录
const columns = [
  { title: t('hr.attendance.employee'), key: 'employee', width: 180, fixed: 'left' },
  { title: t('common.field.date'), key: 'workDate', width: 140 },
  { title: t('hr.attendance.col.checkIn'), key: 'checkIn', width: 150 },
  { title: t('hr.attendance.col.checkOut'), key: 'checkOut', width: 150 },
  { title: t('hr.attendance.col.workHours'), dataIndex: 'workHours', width: 120, customRender: ({ text }) => t('hr.attendance.hoursN', { n: Number(text) }) },
  { title: t('common.field.status'), key: 'status', width: 100, dict: 'hr_attendance_status' },
  { title: t('hr.attendance.col.source'), key: 'source', width: 160 },
  { title: t('common.action.operation'), key: 'action', width: 120, fixed: 'right' }
]
const range = ref([dayjs().startOf('month').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')])
const query = reactive({ keyword: undefined, deptId: undefined, status: undefined })
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageAttendance, query, {
  params: (q) => ({ ...q, beginDate: range.value?.[0], endDate: range.value?.[1] })
})

// 补录
const manualOpen = ref(false)
const saving = ref(false)
const manualRef = ref()
const manualForm = reactive({})
function openManual(record) {
  Object.assign(manualForm, record
    ? { employeeId: record.employeeId, employeeLabel: `${record.empNo} ${record.employeeName}`, workDate: record.workDate,
        checkIn: record.checkIn ? dayjs(record.checkIn).format('HH:mm') : undefined,
        checkOut: record.checkOut ? dayjs(record.checkOut).format('HH:mm') : undefined, remark: '' }
    : { employeeId: undefined, employeeLabel: undefined, workDate: dayjs().format('YYYY-MM-DD'), checkIn: '09:00', checkOut: '18:00', remark: '' })
  manualOpen.value = true
}
async function saveManual() {
  await manualRef.value.validate()
  if (!manualForm.checkIn && !manualForm.checkOut) {
    message.warning(t('hr.attendance.needTime'))
    return
  }
  saving.value = true
  try {
    const at = (t) => (t ? `${manualForm.workDate} ${t}:00` : null)
    await saveAttendance({ employeeId: manualForm.employeeId, workDate: manualForm.workDate, checkIn: at(manualForm.checkIn),
      checkOut: at(manualForm.checkOut), remark: manualForm.remark })
    message.success(t('hr.attendance.manualSaved'))
    manualOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}
async function removeRecord(id) {
  await deleteAttendance(id)
  message.success(t('common.msg.deleteSuccess'))
  load()
}

// 月度汇总
const period = ref(Number(dayjs().format('YYYYMM')))
const summaryDept = ref()
const summaryKeyword = ref()
const summary = ref([])
const summaryLoading = ref(false)
const summaryColumns = computed(() => [
  { title: t('hr.attendance.employee'), key: 'employee', width: 180, fixed: 'left' },
  { title: t('hr.attendance.col.monthDays'), dataIndex: 'monthDays', width: 100, align: 'right' },
  { title: t('hr.attendance.col.shouldDays'), dataIndex: 'shouldDays', width: 120, align: 'right' },
  { title: t('hr.attendance.col.actualDays'), dataIndex: 'actualDays', width: 110, align: 'right' },
  { title: t('hr.attendance.col.late'), dataIndex: 'lateCount', width: 70, align: 'right' },
  { title: t('hr.attendance.col.early'), dataIndex: 'earlyCount', width: 100, align: 'right' },
  { title: t('hr.attendance.col.missing'), dataIndex: 'missingCount', width: 120, align: 'right' },
  { title: t('hr.attendance.col.absent'), key: 'absentDays', width: 120, align: 'right' },
  ...dict.hr_leave_type.map((d) => ({ title: t('hr.attendance.col.leaveHours', { name: d.label }), key: `leave_${d.value}`, width: 130, align: 'right' })),
  { title: t('hr.attendance.col.overtime'), key: 'overtime', width: 120, align: 'right' }
])
async function loadSummary() {
  summaryLoading.value = true
  try {
    summary.value = await attendanceSummary({ period: period.value, deptId: summaryDept.value || undefined, keyword: summaryKeyword.value || undefined })
  } finally {
    summaryLoading.value = false
  }
}

// 班次
const shifts = ref([])
const shiftColumns = [
  { title: t('hr.attendance.shift'), key: 'name' },
  { title: t('hr.attendance.col.time'), key: 'time', width: 120 },
  { title: t('hr.attendance.col.flex'), dataIndex: 'flexMinutes', width: 80, customRender: ({ text }) => t('hr.attendance.minN', { n: text }) },
  { title: t('hr.attendance.col.hours'), dataIndex: 'workHours', width: 70, customRender: ({ text }) => t('hr.attendance.hN', { n: Number(text) }) },
  { title: '', key: 'action', width: 110 }
]
async function loadShifts() {
  shifts.value = await listShift()
}
const shiftOpen = ref(false)
const shiftRef = ref()
const shiftForm = reactive({})
function openShift(s) {
  Object.assign(shiftForm, s
    ? { ...s, isDefaultBool: s.isDefault === 1 }
    : { id: undefined, name: '', startTime: '09:00', endTime: '18:00', flexMinutes: 10, workHours: 8, isDefaultBool: false })
  shiftOpen.value = true
}
async function saveShift() {
  await shiftRef.value.validate()
  saving.value = true
  try {
    const data = { name: shiftForm.name, startTime: shiftForm.startTime, endTime: shiftForm.endTime, flexMinutes: shiftForm.flexMinutes,
      workHours: shiftForm.workHours, isDefault: shiftForm.isDefaultBool ? 1 : 0 }
    if (shiftForm.id) await updateShift(shiftForm.id, data)
    else await addShift(data)
    message.success(t('common.msg.saved'))
    shiftOpen.value = false
    loadShifts()
  } finally {
    saving.value = false
  }
}
async function removeShift(id) {
  await deleteShift(id)
  message.success(t('common.msg.deleteSuccess'))
  loadShifts()
}

// 节假日
const holidayYear = ref(dayjs().year())
const years = [-1, 0, 1].map((d) => ({ value: dayjs().year() + d, label: t('hr.attendance.yearOption', { year: dayjs().year() + d }) }))
const holidays = ref([])
const holidayColumns = [
  { title: t('common.field.date'), key: 'date', width: 150 },
  { title: t('common.field.name'), dataIndex: 'name' },
  { title: t('common.field.type'), key: 'type', width: 110, dict: 'hr_holiday_type' },
  { title: '', key: 'action', width: 110 }
]
async function loadHolidays() {
  holidays.value = await listHoliday(holidayYear.value)
}
const holidayOpen = ref(false)
const holidayRef = ref()
const holidayForm = reactive({})
function openHoliday(h) {
  Object.assign(holidayForm, h ? { ...h } : { id: undefined, holidayDate: undefined, type: 'HOLIDAY', name: '' })
  holidayOpen.value = true
}
async function saveHoliday() {
  await holidayRef.value.validate()
  saving.value = true
  try {
    const data = { holidayDate: holidayForm.holidayDate, type: holidayForm.type, name: holidayForm.name }
    if (holidayForm.id) await updateHoliday(holidayForm.id, data)
    else await addHoliday(data)
    message.success(t('common.msg.saved'))
    holidayOpen.value = false
    loadHolidays()
  } finally {
    saving.value = false
  }
}
async function removeHoliday(id) {
  await deleteHoliday(id)
  message.success(t('common.msg.deleteSuccess'))
  loadHolidays()
}

onMounted(() => {
  load()
  loadSummary()
  loadShifts()
  loadHolidays()
})
</script>

<style scoped>
.head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}
.w100 {
  width: 100px;
}
.w120 {
  width: 120px;
}
.w160 {
  width: 160px;
}
.w180 {
  width: 180px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.warn {
  color: #fa8c16;
  font-size: 12px;
  margin-left: 4px;
}
.danger {
  color: #ff4d4f;
  font-weight: 600;
}
.full {
  width: 100%;
}
.config {
  margin-top: 0;
}
</style>
