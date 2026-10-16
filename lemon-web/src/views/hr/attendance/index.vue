<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card">
      <div class="head">
        <a-segmented v-model:value="tab" :options="TABS" />
        <template v-if="tab === 'records'">
          <a-input v-model:value="query.keyword" placeholder="工号或姓名" allow-clear class="w160" @press-enter="search" />
          <DeptSelect v-model:value="query.deptId" placeholder="全部部门" class="w180" @update:value="search" />
          <a-select v-model:value="query.status" placeholder="全部状态" allow-clear class="w120" :options="toOptions(dict.hr_attendance_status)" @change="search" />
          <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" @change="search" />
          <a-button type="primary" @click="search">查询</a-button>
        </template>
        <template v-else-if="tab === 'summary'">
          <PeriodPicker v-model:value="period" @update:value="loadSummary" />
          <DeptSelect v-model:value="summaryDept" placeholder="全部部门" class="w180" @update:value="loadSummary" />
          <a-input v-model:value="summaryKeyword" placeholder="工号或姓名" allow-clear class="w160" @press-enter="loadSummary" />
          <a-button type="primary" @click="loadSummary">查询</a-button>
        </template>
        <template v-else>
          <span class="sub-text">工作日 = 周一至周五 + 调休上班 - 法定节假日；员工未指定班次时使用默认班次</span>
        </template>
      </div>
    </a-card>

    <!-- 考勤记录 -->
    <a-card v-show="tab === 'records'" :bordered="false" class="page-card table-card">
      <TableToolbar
        :exporter="exportAll" v-model:size="size" :columns="columns" @refresh="load">
        <a-button v-perm="'hr:attendance:edit'" type="primary" @click="openManual()">
          <template #icon><PlusOutlined /></template>补录考勤
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
            <span v-if="record.lateMinutes" class="warn">迟到 {{ record.lateMinutes }} 分</span>
          </template>
          <template v-else-if="column.key === 'checkOut'">
            {{ timeText(record.checkOut) }}
            <span v-if="record.earlyMinutes" class="warn">早退 {{ record.earlyMinutes }} 分</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="hr_attendance_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'source'">
            <a-tag v-if="record.source === 'MANUAL'" color="purple" :bordered="false">补录</a-tag>
            <span v-else class="sub-text">打卡</span>
            <div v-if="record.remark" class="sub-text">{{ record.remark }}</div>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button v-perm="'hr:attendance:edit'" type="link" size="small" @click="openManual(record)">补卡</a-button>
              <span v-perm="'hr:attendance:edit'">
                <a-popconfirm title="删除该考勤记录？" @confirm="removeRecord(record.id)">
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
        :pagination="{ pageSize: 50, showTotal: (t) => `共 ${t} 人` }"
        :scroll="{ x: 1300, y: '100%' }"
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
        <a-card :bordered="false" class="page-card" title="班次">
          <template #extra>
            <a-button v-perm="'hr:attendance:config'" size="small" type="primary" @click="openShift()">新增班次</a-button>
          </template>
          <a-table row-key="id" size="small" :columns="shiftColumns" :data-source="shifts" :pagination="false">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'name'">
                {{ record.name }} <a-tag v-if="record.isDefault" color="blue" :bordered="false">默认</a-tag>
              </template>
              <template v-else-if="column.key === 'time'">{{ record.startTime }} - {{ record.endTime }}</template>
              <template v-else-if="column.key === 'action'">
                <span v-if="userStore.hasPerm('hr:attendance:config')" class="table-actions">
                  <a-button type="link" size="small" @click="openShift(record)">编辑</a-button>
                  <a-popconfirm v-if="!record.isDefault" title="删除该班次？" @confirm="removeShift(record.id)">
                    <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
                  </a-popconfirm>
                </span>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="13">
        <a-card :bordered="false" class="page-card" title="节假日与调休">
          <template #extra>
            <a-space>
              <a-select v-model:value="holidayYear" :options="years" class="w100" @change="loadHolidays" />
              <a-button v-perm="'hr:attendance:config'" size="small" type="primary" @click="openHoliday()">新增</a-button>
            </a-space>
          </template>
          <a-table row-key="id" size="small" :columns="holidayColumns" :data-source="holidays" :pagination="{ pageSize: 15 }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'date'">{{ record.holidayDate }} <span class="sub-text">{{ weekday(record.holidayDate) }}</span></template>
              <template v-else-if="column.key === 'type'"><DictTag type="hr_holiday_type" :value="record.type" /></template>
              <template v-else-if="column.key === 'action'">
                <span v-if="userStore.hasPerm('hr:attendance:config')" class="table-actions">
                  <a-button type="link" size="small" @click="openHoliday(record)">编辑</a-button>
                  <a-popconfirm title="删除该日期设置？" @confirm="removeHoliday(record.id)">
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
    <a-modal v-model:open="manualOpen" title="补录考勤" :confirm-loading="saving" destroy-on-close @ok="saveManual">
      <a-form ref="manualRef" :model="manualForm" layout="vertical">
        <a-form-item label="员工" name="employeeId" :rules="[{ required: true, message: '请选择员工' }]">
          <EmployeeSelect v-model:value="manualForm.employeeId" :label="manualForm.employeeLabel" />
        </a-form-item>
        <a-form-item label="日期" name="workDate" :rules="[{ required: true, message: '请选择日期' }]">
          <a-date-picker v-model:value="manualForm.workDate" value-format="YYYY-MM-DD" :disabled-date="(d) => d.isAfter(dayjs(), 'day')" class="full" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="上班时间" name="checkIn">
              <a-time-picker v-model:value="manualForm.checkIn" format="HH:mm" value-format="HH:mm" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="下班时间" name="checkOut">
              <a-time-picker v-model:value="manualForm.checkOut" format="HH:mm" value-format="HH:mm" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="补录说明" name="remark" :rules="[{ required: true, whitespace: true, message: '请填写补录说明' }]">
          <a-input v-model:value="manualForm.remark" :maxlength="255" placeholder="如：外出拜访客户、忘记打卡" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 班次 -->
    <a-modal v-model:open="shiftOpen" :title="shiftForm.id ? '编辑班次' : '新增班次'" :confirm-loading="saving" destroy-on-close @ok="saveShift">
      <a-form ref="shiftRef" :model="shiftForm" layout="vertical">
        <a-form-item label="班次名称" name="name" :rules="[{ required: true, whitespace: true, message: '请输入班次名称' }]">
          <a-input v-model:value="shiftForm.name" :maxlength="64" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="上班时间" name="startTime" :rules="[{ required: true, message: '请选择上班时间' }]">
              <a-time-picker v-model:value="shiftForm.startTime" format="HH:mm" value-format="HH:mm" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="下班时间" name="endTime" :rules="[{ required: true, message: '请选择下班时间' }]">
              <a-time-picker v-model:value="shiftForm.endTime" format="HH:mm" value-format="HH:mm" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="迟到 / 早退宽限" name="flexMinutes">
              <a-input-number v-model:value="shiftForm.flexMinutes" :min="0" :max="120" addon-after="分钟" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="标准工时" name="workHours" extra="请假按此折算天数">
              <a-input-number v-model:value="shiftForm.workHours" :min="1" :max="12" :step="0.5" addon-after="小时" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item name="isDefault">
          <a-checkbox v-model:checked="shiftForm.isDefaultBool">设为默认班次</a-checkbox>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 节假日 -->
    <a-modal v-model:open="holidayOpen" :title="holidayForm.id ? '编辑日期' : '新增节假日 / 调休'" :confirm-loading="saving" destroy-on-close @ok="saveHoliday">
      <a-form ref="holidayRef" :model="holidayForm" layout="vertical">
        <a-form-item label="日期" name="holidayDate" :rules="[{ required: true, message: '请选择日期' }]">
          <a-date-picker v-model:value="holidayForm.holidayDate" value-format="YYYY-MM-DD" class="full" />
        </a-form-item>
        <a-form-item label="类型" name="type" :rules="[{ required: true, message: '请选择类型' }]">
          <a-radio-group v-model:value="holidayForm.type" :options="toOptions(dict.hr_holiday_type)" />
        </a-form-item>
        <a-form-item label="名称" name="name" :rules="[{ required: true, whitespace: true, message: '请填写名称' }]">
          <a-input v-model:value="holidayForm.name" :maxlength="64" placeholder="如：国庆节、国庆节调休" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
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

const userStore = useUserStore()
const dict = useDict('hr_attendance_status', 'hr_leave_type', 'hr_holiday_type')
const TABS = [
  { label: '考勤记录', value: 'records' },
  { label: '月度汇总', value: 'summary' },
  { label: '班次与节假日', value: 'config' }
]
const tab = ref('records')
const WEEK = ['日', '一', '二', '三', '四', '五', '六']
const weekday = (d) => `周${WEEK[dayjs(d).day()]}`

// 考勤记录
const columns = [
  { title: '员工', key: 'employee', width: 180, fixed: 'left' },
  { title: '日期', key: 'workDate', width: 140 },
  { title: '上班', key: 'checkIn', width: 150 },
  { title: '下班', key: 'checkOut', width: 150 },
  { title: '出勤时长', dataIndex: 'workHours', width: 90, customRender: ({ text }) => `${Number(text)} 小时` },
  { title: '状态', key: 'status', width: 100, dict: 'hr_attendance_status' },
  { title: '来源', key: 'source', width: 160 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' }
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
    message.warning('请至少填写上班或下班时间')
    return
  }
  saving.value = true
  try {
    const at = (t) => (t ? `${manualForm.workDate} ${t}:00` : null)
    await saveAttendance({ employeeId: manualForm.employeeId, workDate: manualForm.workDate, checkIn: at(manualForm.checkIn),
      checkOut: at(manualForm.checkOut), remark: manualForm.remark })
    message.success('已补录')
    manualOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}
async function removeRecord(id) {
  await deleteAttendance(id)
  message.success('删除成功')
  load()
}

// 月度汇总
const period = ref(Number(dayjs().format('YYYYMM')))
const summaryDept = ref()
const summaryKeyword = ref()
const summary = ref([])
const summaryLoading = ref(false)
const summaryColumns = computed(() => [
  { title: '员工', key: 'employee', width: 180, fixed: 'left' },
  { title: '工作日', dataIndex: 'monthDays', width: 80, align: 'right' },
  { title: '应出勤', dataIndex: 'shouldDays', width: 80, align: 'right' },
  { title: '实际出勤', dataIndex: 'actualDays', width: 90, align: 'right' },
  { title: '迟到', dataIndex: 'lateCount', width: 70, align: 'right' },
  { title: '早退', dataIndex: 'earlyCount', width: 70, align: 'right' },
  { title: '缺卡', dataIndex: 'missingCount', width: 70, align: 'right' },
  { title: '旷工(天)', key: 'absentDays', width: 90, align: 'right' },
  ...dict.hr_leave_type.map((t) => ({ title: `${t.label}(时)`, key: `leave_${t.value}`, width: 100, align: 'right' })),
  { title: '加班(时)', key: 'overtime', width: 90, align: 'right' }
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
  { title: '班次', key: 'name' },
  { title: '时间', key: 'time', width: 120 },
  { title: '宽限', dataIndex: 'flexMinutes', width: 70, customRender: ({ text }) => `${text} 分` },
  { title: '工时', dataIndex: 'workHours', width: 70, customRender: ({ text }) => `${Number(text)} 时` },
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
    message.success('已保存')
    shiftOpen.value = false
    loadShifts()
  } finally {
    saving.value = false
  }
}
async function removeShift(id) {
  await deleteShift(id)
  message.success('删除成功')
  loadShifts()
}

// 节假日
const holidayYear = ref(dayjs().year())
const years = [-1, 0, 1].map((d) => ({ value: dayjs().year() + d, label: `${dayjs().year() + d} 年` }))
const holidays = ref([])
const holidayColumns = [
  { title: '日期', key: 'date', width: 150 },
  { title: '名称', dataIndex: 'name' },
  { title: '类型', key: 'type', width: 110, dict: 'hr_holiday_type' },
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
    message.success('已保存')
    holidayOpen.value = false
    loadHolidays()
  } finally {
    saving.value = false
  }
}
async function removeHoliday(id) {
  await deleteHoliday(id)
  message.success('删除成功')
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
