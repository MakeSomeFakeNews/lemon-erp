<template>
  <div class="self-page">
    <a-spin :spinning="loading">
      <a-card v-if="profile && !profile.employee" :bordered="false" class="page-card">
        <a-result status="info" :title="$t('hr.self.noEmployee')" :sub-title="$t('hr.self.noEmployeeTip')">
          <template v-if="profile.pendingReviews" #extra>
            <a-button type="primary" @click="tab = 'perf'">{{ $t('hr.self.pendingReviewsBtn', { n: profile.pendingReviews }) }}</a-button>
          </template>
        </a-result>
      </a-card>

      <template v-if="employee">
        <a-row :gutter="16">
          <a-col :xs="24" :lg="16">
            <a-card :bordered="false" class="page-card profile-card">
              <div class="profile">
                <a-avatar :size="56" class="avatar">{{ employee.name.slice(-2) }}</a-avatar>
                <div class="profile-main">
                  <div class="profile-name">
                    {{ employee.name }} <DictTag type="hr_emp_status" :value="employee.status" />
                  </div>
                  <div class="sub-text">
                    {{ employee.empNo }} · {{ employee.deptName }} · {{ employee.positionName || $t('hr.employee.noPosition') }} · {{ $t('hr.self.entryOn', { date: employee.entryDate }) }}
                  </div>
                </div>
              </div>
              <a-row :gutter="12" class="stats">
                <a-col :span="6">
                  <div class="stat-label">{{ $t('hr.self.annualLeft') }}</div>
                  <div class="stat-value">{{ annualLeft }} <small>{{ $t('hr.self.daysUnit') }}</small></div>
                  <div class="sub-text">{{ $t('hr.self.annualTotal', { total: profile.annualDays, used: Number(profile.annualUsedDays) }) }}</div>
                  <div class="sub-text">{{ $t('hr.self.compLeft', { n: Number(profile.compensatoryHours) }) }}</div>
                </a-col>
                <a-col :span="6">
                  <div class="stat-label">{{ $t('hr.self.trainingThisYear') }}</div>
                  <div class="stat-value">{{ Number(profile.trainingHours) }} <small>{{ $t('hr.self.creditHours') }}</small></div>
                </a-col>
                <a-col :span="6">
                  <div class="stat-label">{{ $t('hr.self.pendingSelf') }}</div>
                  <div class="stat-value" :class="{ warn: profile.pendingSelf }">{{ profile.pendingSelf }}</div>
                </a-col>
                <a-col :span="6">
                  <div class="stat-label">{{ $t('hr.self.pendingReview') }}</div>
                  <div class="stat-value" :class="{ warn: profile.pendingReviews }">{{ profile.pendingReviews }}</div>
                </a-col>
              </a-row>
            </a-card>
          </a-col>
          <a-col :xs="24" :lg="8">
            <a-card :bordered="false" class="page-card clock-card">
              <div class="clock-time">{{ now.format('HH:mm:ss') }}</div>
              <div class="sub-text">{{ now.format($t('hr.self.dateFormat')) }}</div>
              <div class="clock-records">
                <span>{{ $t('hr.self.checkIn') }} <b>{{ timeText(todayRecord?.checkIn) }}</b></span>
                <span>{{ $t('hr.self.checkOut') }} <b>{{ timeText(todayRecord?.checkOut) }}</b></span>
                <DictTag v-if="todayRecord" type="hr_attendance_status" :value="todayRecord.status" />
              </div>
              <a-button type="primary" size="large" shape="round" :loading="clocking" class="clock-btn" @click="clock">
                {{ todayRecord?.checkIn ? $t('hr.self.clockOut') : $t('hr.self.clockIn') }}
              </a-button>
            </a-card>
          </a-col>
        </a-row>
      </template>

      <a-card v-if="profile" :bordered="false" class="page-card tabs-card">
        <a-tabs v-model:active-key="tab">
          <a-tab-pane v-if="employee" key="calendar" :tab="$t('hr.self.calendar')">
            <div class="cal-head">
              <a-button size="small" @click="shiftMonth(-1)"><LeftOutlined /></a-button>
              <b>{{ periodText(period) }}</b>
              <a-button size="small" :disabled="period >= currentPeriod" @click="shiftMonth(1)"><RightOutlined /></a-button>
              <span v-if="summary" class="sub-text">
                {{ $t('hr.self.summary1', { should: summary.shouldDays, actual: summary.actualDays, late: summary.lateCount, early: summary.earlyCount }) }}
                · {{ $t('hr.self.absentLabel') }} <span :class="{ danger: summary.absentDays }">{{ summary.absentDays }}</span> {{ $t('hr.self.daysUnit') }}
                · {{ $t('hr.self.overtimeHours', { n: Number(summary.overtimeHours) }) }}
              </span>
            </div>
            <div class="calendar">
              <div v-for="w in WEEK" :key="w" class="cal-week">{{ w }}</div>
              <div v-for="n in leading" :key="`b${n}`" class="cal-cell empty" />
              <div
                v-for="d in calendar"
                :key="d.date"
                class="cal-cell"
                :class="{ rest: !d.workday, today: d.date === todayStr, absent: d.absent }"
              >
                <div class="cal-date">
                  {{ Number(d.date.slice(8)) }}
                  <span v-if="d.holidayName" class="holiday">{{ d.holidayName }}</span>
                </div>
                <template v-if="d.attendance">
                  <div class="cal-time">{{ timeText(d.attendance.checkIn) }} - {{ timeText(d.attendance.checkOut) }}</div>
                  <DictTag v-if="d.attendance.status !== 'NORMAL'" type="hr_attendance_status" :value="d.attendance.status" />
                </template>
                <div v-for="l in d.leaves" :key="l.id" class="cal-leave" :class="{ pending: l.status === 'APPROVING' }">
                  {{ l.kind === 'OVERTIME' ? (l.type === 'TO_LEAVE' ? $t('hr.self.overtimeToLeave') : $t('hr.self.overtime')) : dictLabel(dict.hr_leave_type, l.type) }} {{ Number(l.hours) }}h
                </div>
                <div v-if="d.absent" class="cal-absent">{{ $t('hr.self.absentLabel') }}</div>
              </div>
            </div>
          </a-tab-pane>

          <a-tab-pane v-if="employee" key="leave" :tab="$t('hr.self.leaveOvertime')">
            <div class="tab-head">
              <a-button type="primary" @click="openApply('LEAVE')">{{ $t('hr.self.applyLeave') }}</a-button>
              <a-button @click="openApply('OVERTIME')">{{ $t('hr.self.applyOvertime') }}</a-button>
            </div>
            <a-table row-key="id" size="small" :columns="leaveColumns" :data-source="leaves" :pagination="{ pageSize: 10 }" :scroll="{ x: leaveScrollX }">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'kind'">
                  <DictTag type="hr_leave_kind" :value="record.kind" />
                  <DictTag v-if="record.type" type="hr_leave_type" :value="record.type" />
                </template>
                <template v-else-if="column.key === 'time'">{{ $t('hr.self.timeRange', { start: fmt(record.startTime), end: fmt(record.endTime) }) }}</template>
                <template v-else-if="column.key === 'hours'">{{ hoursText(record.hours) }}</template>
                <template v-else-if="column.key === 'status'">
                  <DictTag type="hr_leave_status" :value="record.status" />
                  <div v-if="record.approveComment" class="sub-text">{{ record.approveComment }}</div>
                </template>
                <template v-else-if="column.key === 'action'">
                  <a-popconfirm v-if="canCancel(record)" :title="$t('hr.self.confirmCancel')" @confirm="cancel(record.id)">
                    <a-button type="link" size="small">{{ $t('hr.self.cancel') }}</a-button>
                  </a-popconfirm>
                </template>
              </template>
            </a-table>
          </a-tab-pane>

          <a-tab-pane v-if="employee" key="payslip" :tab="$t('hr.self.payslip')">
            <a-empty v-if="!payslips.length" :description="$t('hr.self.noPayslips')" />
            <a-row v-else :gutter="16">
              <a-col :xs="24" :md="7">
                <div v-for="s in payslips" :key="s.id" class="slip-item" :class="{ active: s.id === slip?.id }" @click="slip = s">
                  <b>{{ periodText(s.period) }}</b>
                  <span>{{ formatMoney(s.netPay) }}</span>
                </div>
              </a-col>
              <a-col v-if="slip" :xs="24" :md="17">
                <a-descriptions :title="$t('hr.self.payslipTitle', { period: periodText(slip.period) })" bordered size="small" :column="2">
                  <a-descriptions-item :label="$t('hr.self.baseSalary')">{{ formatMoney(slip.baseSalary) }}</a-descriptions-item>
                  <a-descriptions-item :label="$t('hr.self.allowance')">{{ formatMoney(slip.allowance) }}</a-descriptions-item>
                  <a-descriptions-item :label="$t('hr.self.overtimePay')">{{ formatMoney(slip.overtimePay) }} {{ $t('hr.self.hoursParen', { n: Number(slip.overtimeHours) }) }}</a-descriptions-item>
                  <a-descriptions-item :label="$t('hr.self.perfBonus')">
                    {{ formatMoney(slip.perfBonus) }} <DictTag v-if="slip.perfGrade" type="hr_perf_grade" :value="slip.perfGrade" />
                  </a-descriptions-item>
                  <a-descriptions-item v-if="Number(slip.pieceWage)" :label="$t('hr.self.pieceWage')">{{ formatMoney(slip.pieceWage) }}</a-descriptions-item>
                  <a-descriptions-item v-if="Number(slip.commission)" :label="$t('hr.self.commission')">{{ formatMoney(slip.commission) }}</a-descriptions-item>
                  <a-descriptions-item :label="$t('hr.self.otherBonus')">{{ formatMoney(slip.otherBonus) }}</a-descriptions-item>
                  <a-descriptions-item :label="$t('hr.self.attendanceDeduct')">
                    -{{ formatMoney(slip.attendanceDeduct) }}
                    <span class="sub-text">{{ $t('hr.self.deductDetail', { absent: Number(slip.absentDays), late: slip.lateCount }) }}</span>
                  </a-descriptions-item>
                  <a-descriptions-item :label="$t('hr.self.grossPay')" :span="2"><b>{{ formatMoney(slip.grossPay) }}</b></a-descriptions-item>
                  <a-descriptions-item :label="$t('hr.self.socialPersonal')">-{{ formatMoney(slip.socialPersonal) }}</a-descriptions-item>
                  <a-descriptions-item :label="$t('hr.self.housingPersonal')">-{{ formatMoney(slip.housingPersonal) }}</a-descriptions-item>
                  <a-descriptions-item :label="$t('hr.self.incomeTax')">-{{ formatMoney(slip.incomeTax) }}</a-descriptions-item>
                  <a-descriptions-item :label="$t('hr.self.otherDeduct')">-{{ formatMoney(slip.otherDeduct) }}</a-descriptions-item>
                  <a-descriptions-item :label="$t('hr.self.netPay')" :span="2"><b class="net">{{ formatMoney(slip.netPay) }}</b></a-descriptions-item>
                  <a-descriptions-item :label="$t('hr.self.specialDeduct')">{{ formatMoney(slip.specialDeduct) }}</a-descriptions-item>
                  <a-descriptions-item :label="$t('hr.self.cumTaxable')">{{ formatMoney(slip.cumTaxable) }}</a-descriptions-item>
                  <a-descriptions-item :label="$t('hr.self.cumTax')">{{ formatMoney(slip.cumTax) }}</a-descriptions-item>
                  <a-descriptions-item :label="$t('common.field.status')"><DictTag type="hr_payroll_status" :value="slip.payrollStatus" /></a-descriptions-item>
                </a-descriptions>
              </a-col>
            </a-row>
          </a-tab-pane>

          <a-tab-pane v-if="employee" key="training" :tab="$t('hr.self.myTraining')">
            <a-table row-key="id" size="small" :columns="trainingColumns" :data-source="trainings" :pagination="{ pageSize: 10 }">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'category'"><DictTag type="hr_training_category" :value="record.category" /></template>
                <template v-else-if="column.key === 'status'"><DictTag type="hr_training_status" :value="record.trainingStatus" /></template>
                <template v-else-if="column.key === 'result'">
                  <template v-if="record.trainingStatus === 'FINISHED'">
                    {{ record.attended ? $t('hr.employee.attended') : $t('hr.employee.absent') }}
                    <span v-if="record.score != null"> · {{ $t('hr.recruit.scoreN', { n: record.score }) }}</span>
                    <a-tag v-if="record.passed === 1" color="green" :bordered="false">{{ $t('hr.employee.passed') }}</a-tag>
                    <a-tag v-else-if="record.passed === 0" color="red" :bordered="false">{{ $t('hr.employee.failed') }}</a-tag>
                  </template>
                  <span v-else class="sub-text">-</span>
                </template>
              </template>
            </a-table>
          </a-tab-pane>

          <a-tab-pane key="perf" :tab="$t('hr.self.perf')">
            <template v-if="employee">
              <div class="section-title">{{ $t('hr.self.myAppraisals') }}</div>
              <a-table row-key="id" size="small" :columns="myAppraisalColumns" :data-source="myAppraisals" :pagination="false">
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'status'"><DictTag type="hr_appraisal_status" :value="record.status" /></template>
                  <template v-else-if="column.key === 'grade'"><DictTag type="hr_perf_grade" :value="record.grade" /></template>
                  <template v-else-if="column.key === 'action'">
                    <a-button v-if="record.status === 'SELF' && record.planStatus === 'SCORING'" type="link" size="small" @click="openScore(record, 'self')">
                      {{ $t('hr.scoreModal.self') }}
                    </a-button>
                    <a-button v-else-if="record.status === 'DONE'" type="link" size="small" @click="viewResult(record)">{{ $t('common.action.view') }}</a-button>
                  </template>
                </template>
              </a-table>
            </template>
            <div class="section-title">{{ $t('hr.self.pendingReview') }}</div>
            <a-table row-key="id" size="small" :columns="reviewColumns" :data-source="reviews" :pagination="false">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'employee'">{{ record.empNo }} {{ record.employeeName }}</template>
                <template v-else-if="column.key === 'status'"><DictTag type="hr_appraisal_status" :value="record.status" /></template>
                <template v-else-if="column.key === 'grade'"><DictTag type="hr_perf_grade" :value="record.grade" /></template>
                <template v-else-if="column.key === 'action'">
                  <a-button v-if="record.status !== 'DONE' && record.planStatus === 'SCORING'" type="link" size="small" @click="openScore(record, 'review')">
                    {{ $t('hr.scoreModal.score') }}
                  </a-button>
                </template>
              </template>
            </a-table>
          </a-tab-pane>
        </a-tabs>
      </a-card>
    </a-spin>

    <!-- 请假 / 加班申请 -->
    <a-modal v-model:open="applyOpen" :title="applyForm.kind === 'OVERTIME' ? $t('hr.self.applyOvertime') : $t('hr.self.applyLeave')" :confirm-loading="saving" destroy-on-close @ok="apply">
      <a-form ref="applyRef" :model="applyForm" layout="vertical">
        <a-form-item v-if="applyForm.kind === 'LEAVE'" :label="$t('hr.self.leaveType')" name="type" :rules="[{ required: true, message: $t('hr.self.leaveTypeRequired') }]"
          :extra="applyForm.type === 'COMPENSATORY' ? $t('hr.self.compLeft', { n: Number(profile.compensatoryHours) }) : ''">
          <a-select v-model:value="applyForm.type" :options="leaveTypeOptions" />
        </a-form-item>
        <a-form-item v-else :label="$t('hr.self.compensation')" name="type">
          <a-radio-group v-model:value="applyForm.type">
            <a-radio value="PAY">{{ $t('hr.self.payOvertime') }}</a-radio>
            <a-radio value="TO_LEAVE">{{ $t('hr.self.toLeave') }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('hr.self.time')" name="range" :rules="[{ required: true, message: $t('hr.self.rangeRequired') }]">
          <a-range-picker
            v-model:value="applyForm.range"
            show-time
            :minute-step="30"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm:ss"
            class="full"
            @change="suggestHours"
          />
        </a-form-item>
        <a-form-item :label="$t('hr.self.duration')" name="hours" :rules="[{ required: true, message: $t('hr.self.durationRequired') }]" :extra="hoursHint">
          <a-input-number v-model:value="applyForm.hours" :min="0.5" :step="0.5" :precision="1" :addon-after="$t('hr.self.hoursUnit')" class="full" />
        </a-form-item>
        <a-form-item :label="$t('hr.self.reason')" name="reason">
          <a-textarea v-model:value="applyForm.reason" :rows="3" :maxlength="255" />
        </a-form-item>
      </a-form>
    </a-modal>

    <ScoreModal v-model:open="scoreOpen" :appraisal-id="scoreId" :mode="scoreMode" @success="loadPerf" />

    <a-modal v-model:open="resultOpen" :title="$t('hr.self.resultTitle')" :footer="null" destroy-on-close>
      <template v-if="result">
        <a-descriptions size="small" :column="1" bordered>
          <a-descriptions-item :label="$t('hr.scoreModal.plan')">{{ result.planName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('hr.self.score')">{{ $t('hr.self.scoreWithSelf', { score: result.score, self: result.selfScore ?? '-' }) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('hr.self.grade')"><DictTag type="hr_perf_grade" :value="result.grade" /></a-descriptions-item>
          <a-descriptions-item :label="$t('hr.self.reviewer')">{{ result.reviewerName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('hr.self.comment')">{{ result.reviewComment || '-' }}</a-descriptions-item>
        </a-descriptions>
        <a-table row-key="id" size="small" :columns="scoreColumns" :data-source="result.scores" :pagination="false" class="score-table" />
      </template>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { LeftOutlined, RightOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import DictTag from '@/components/DictTag.vue'
import ScoreModal from '@/components/hr/ScoreModal.vue'
import { hoursText, periodText, timeText } from '@/components/hr/hr'
import { formatMoney } from '@/components/scm/scm'
import {
  applyLeave,
  cancelLeave,
  getAppraisal,
  selfAppraisals,
  selfCalendar,
  selfClock,
  selfLeaves,
  selfPayslips,
  selfProfile,
  selfReviews,
  selfSummary,
  selfTrainings
} from '@/api/hr'
import { useDict } from '@/composables/useDict'
import { findDict, toOptions } from '@/utils/dict'
import { t } from '@/locales'

const dict = useDict('hr_emp_status', 'hr_attendance_status', 'hr_leave_type', 'hr_leave_kind', 'hr_leave_status', 'hr_payroll_status',
  'hr_perf_grade', 'hr_appraisal_status', 'hr_training_category', 'hr_training_status')
const dictLabel = (list, v) => findDict(list, v)?.label || ''
const fmt = (v) => (v ? dayjs(v).format('MM-DD HH:mm') : '')
const WEEK = [0, 1, 2, 3, 4, 5, 6].map((i) => t(`hr.self.week.${i}`))

const loading = ref(false)
const profile = ref()
const employee = computed(() => profile.value?.employee)
const annualLeft = computed(() => Math.max(0, +(profile.value.annualDays - Number(profile.value.annualUsedDays)).toFixed(1)))
const tab = ref('calendar')

// 时钟
const now = ref(dayjs())
let timer
onMounted(() => (timer = setInterval(() => (now.value = dayjs()), 1000)))
onBeforeUnmount(() => clearInterval(timer))
const todayStr = dayjs().format('YYYY-MM-DD')

async function loadProfile() {
  profile.value = await selfProfile()
  if (!profile.value.employee) tab.value = 'perf'
}

// 考勤日历
const currentPeriod = Number(dayjs().format('YYYYMM'))
const period = ref(currentPeriod)
const calendar = ref([])
const summary = ref()
const leading = computed(() => (calendar.value.length ? dayjs(calendar.value[0].date).day() : 0))
const todayRecord = computed(() => calendar.value.find((d) => d.date === todayStr)?.attendance)
async function loadCalendar() {
  const [c, s] = await Promise.all([selfCalendar(period.value), selfSummary(period.value)])
  calendar.value = c
  summary.value = s
}
function shiftMonth(n) {
  period.value = Number(dayjs(`${String(period.value).slice(0, 4)}-${String(period.value).slice(4)}-01`).add(n, 'month').format('YYYYMM'))
  loadCalendar()
}

const clocking = ref(false)
async function clock() {
  clocking.value = true
  try {
    const r = await selfClock()
    message.success(t(r.checkOut ? 'hr.self.clockOutSuccess' : 'hr.self.clockInSuccess', { time: timeText(r.checkOut || r.checkIn) }))
    if (period.value !== currentPeriod) period.value = currentPeriod
    loadCalendar()
  } finally {
    clocking.value = false
  }
}

// 请假加班
const leaves = ref([])
const leaveColumns = [
  { title: t('hr.self.docNo'), dataIndex: 'leaveNo', width: 150 },
  { title: t('hr.self.kind'), key: 'kind', width: 150 },
  { title: t('hr.self.time'), key: 'time', width: 220 },
  { title: t('hr.self.duration'), key: 'hours', width: 140 },
  { title: t('hr.self.reason'), dataIndex: 'reason', width: 200, ellipsis: true },
  { title: t('common.field.status'), key: 'status', width: 140 },
  { title: '', key: 'action', width: 80 }
]
const leaveScrollX = leaveColumns.reduce((sum, c) => sum + c.width, 0)
const canCancel = (r) => r.status === 'APPROVING' || (r.status === 'APPROVED' && dayjs(r.startTime).isAfter(dayjs()))
async function loadLeaves() {
  leaves.value = await selfLeaves()
}
async function cancel(id) {
  await cancelLeave(id)
  message.success(t('hr.self.cancelled'))
  loadLeaves()
  loadCalendar()
  loadProfile()
}

const applyOpen = ref(false)
const saving = ref(false)
const applyRef = ref()
const applyForm = reactive({ kind: 'LEAVE', type: undefined, range: undefined, hours: undefined, reason: '' })
const leaveTypeOptions = computed(() => toOptions(dict.hr_leave_type).map((o) => ({
  ...o, label: o.value === 'ANNUAL' && profile.value ? t('hr.self.annualOption', { label: o.label, n: annualLeft.value }) : o.label
})))
const hoursHint = computed(() => (applyForm.kind === 'LEAVE' ? t('hr.self.leaveHint') : t('hr.self.overtimeHint')))
function openApply(kind) {
  const start = dayjs().add(1, 'day').hour(kind === 'LEAVE' ? 9 : 18).minute(kind === 'LEAVE' ? 0 : 30).second(0)
  const end = kind === 'LEAVE' ? start.hour(18) : start.add(2, 'hour')
  Object.assign(applyForm, { kind, type: kind === 'LEAVE' ? 'PERSONAL' : 'PAY',
    range: [start.format('YYYY-MM-DD HH:mm:ss'), end.format('YYYY-MM-DD HH:mm:ss')], hours: undefined, reason: '' })
  suggestHours()
  applyOpen.value = true
}
/** 请假：按日期内的工作日（周一至周五）× 8 小时估算；加班：按起止时间差 */
function suggestHours() {
  const [s, e] = applyForm.range || []
  if (!s || !e) return
  const start = dayjs(s)
  const end = dayjs(e)
  if (!end.isAfter(start)) return
  if (applyForm.kind === 'OVERTIME') {
    applyForm.hours = Math.round((end.diff(start, 'minute') / 60) * 2) / 2
    return
  }
  if (start.isSame(end, 'day')) {
    const minutes = end.diff(start, 'minute') - (start.hour() < 13 && end.hour() >= 13 ? 60 : 0)
    applyForm.hours = Math.min(8, Math.max(0.5, Math.round((minutes / 60) * 2) / 2))
    return
  }
  let days = 0
  for (let d = start.startOf('day'); !d.isAfter(end, 'day'); d = d.add(1, 'day')) {
    if (d.day() !== 0 && d.day() !== 6) days++
  }
  applyForm.hours = days * 8
}
async function apply() {
  await applyRef.value.validate()
  saving.value = true
  try {
    await applyLeave({ kind: applyForm.kind, type: applyForm.type || null, startTime: applyForm.range[0], endTime: applyForm.range[1],
      hours: applyForm.hours, reason: applyForm.reason || null })
    message.success(t('hr.self.submitted'))
    applyOpen.value = false
    loadLeaves()
    loadCalendar()
    loadProfile()
  } finally {
    saving.value = false
  }
}

// 工资条、培训
const payslips = ref([])
const slip = ref()
const trainings = ref([])
const trainingColumns = [
  { title: t('hr.employee.training'), dataIndex: 'trainingTitle' },
  { title: t('hr.position.category'), key: 'category', width: 120 },
  { title: t('common.field.date'), dataIndex: 'startDate', width: 110 },
  { title: t('hr.employee.hours'), dataIndex: 'hours', width: 80 },
  { title: t('common.field.status'), key: 'status', width: 110 },
  { title: t('hr.employee.result'), key: 'result', width: 200 }
]

// 绩效
const myAppraisals = ref([])
const reviews = ref([])
const myAppraisalColumns = [
  { title: t('hr.scoreModal.plan'), dataIndex: 'planName' },
  { title: t('hr.self.reviewer'), dataIndex: 'reviewerName', width: 100 },
  { title: t('hr.scoreModal.self'), dataIndex: 'selfScore', width: 80 },
  { title: t('hr.self.score'), dataIndex: 'score', width: 80 },
  { title: t('hr.self.grade'), key: 'grade', width: 90 },
  { title: t('common.field.status'), key: 'status', width: 110 },
  { title: '', key: 'action', width: 80 }
]
const reviewColumns = [
  { title: t('hr.scoreModal.plan'), dataIndex: 'planName' },
  { title: t('hr.scoreModal.appraisee'), key: 'employee', width: 160 },
  { title: t('common.field.dept'), dataIndex: 'deptName', width: 120 },
  { title: t('hr.scoreModal.self'), dataIndex: 'selfScore', width: 80 },
  { title: t('hr.self.score'), dataIndex: 'score', width: 80 },
  { title: t('hr.self.grade'), key: 'grade', width: 90 },
  { title: t('common.field.status'), key: 'status', width: 110 },
  { title: '', key: 'action', width: 80 }
]
async function loadPerf() {
  const tasks = [selfReviews({ current: 1, size: 100 })]
  if (employee.value) tasks.push(selfAppraisals({ current: 1, size: 50 }))
  const [r, mine] = await Promise.all(tasks)
  reviews.value = r.records
  myAppraisals.value = mine ? mine.records : []
  if (profile.value) loadProfile()
}
const scoreOpen = ref(false)
const scoreId = ref()
const scoreMode = ref('self')
function openScore(record, mode) {
  scoreId.value = record.id
  scoreMode.value = mode
  scoreOpen.value = true
}
const resultOpen = ref(false)
const result = ref()
const scoreColumns = [
  { title: t('hr.scoreModal.indicator'), dataIndex: 'indicatorName' },
  { title: t('hr.scoreModal.weight'), dataIndex: 'weight', width: 80, customRender: ({ text }) => `${Number(text)}%` },
  { title: t('hr.scoreModal.self'), dataIndex: 'selfScore', width: 80 },
  { title: t('hr.scoreModal.score'), dataIndex: 'score', width: 80 }
]
async function viewResult(record) {
  result.value = await getAppraisal(record.id)
  resultOpen.value = true
}

onMounted(async () => {
  loading.value = true
  try {
    await loadProfile()
    if (employee.value) {
      const [l, p, t] = await Promise.all([selfLeaves(), selfPayslips(), selfTrainings(), loadCalendar()])
      leaves.value = l
      payslips.value = p
      slip.value = p[0]
      trainings.value = t
    }
    await loadPerf()
  } finally {
    loading.value = false
  }
})
watch(tab, (t) => {
  if (t === 'perf') loadPerf()
})
</script>

<style scoped>
.self-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.profile-card,
.clock-card {
  height: 100%;
  margin-bottom: 16px;
}
.profile {
  display: flex;
  gap: 16px;
  align-items: center;
}
.avatar {
  background: #1677ff;
  flex-shrink: 0;
}
.profile-name {
  font-size: 18px;
  font-weight: 600;
}
.stats {
  margin-top: 20px;
}
.stat-label {
  color: var(--app-text-3);
  font-size: 12px;
}
.stat-value {
  font-size: 22px;
  font-weight: 600;
}
.stat-value small {
  font-size: 12px;
  font-weight: 400;
  color: var(--app-text-3);
}
.stat-value.warn {
  color: #fa8c16;
}
.clock-card {
  text-align: center;
}
.clock-time {
  font-size: 34px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
.clock-records {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin: 12px 0;
  align-items: center;
}
.clock-btn {
  min-width: 160px;
}
.tabs-card :deep(.ant-card-body) {
  padding-top: 8px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.danger {
  color: #ff4d4f;
  font-weight: 600;
}
.cal-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.calendar {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  border-top: 1px solid var(--app-border);
  border-left: 1px solid var(--app-border);
}
.cal-week {
  padding: 6px;
  text-align: center;
  font-weight: 600;
  background: var(--app-hover);
  border-right: 1px solid var(--app-border);
  border-bottom: 1px solid var(--app-border);
}
.cal-cell {
  min-height: 92px;
  padding: 6px;
  border-right: 1px solid var(--app-border);
  border-bottom: 1px solid var(--app-border);
  font-size: 12px;
  overflow: hidden;
}
.cal-cell.rest {
  background: var(--app-hover);
}
.cal-cell.today {
  box-shadow: inset 0 0 0 2px #1677ff;
}
.cal-cell.absent {
  background: rgba(255, 77, 79, 0.08);
}
.cal-date {
  font-weight: 600;
  margin-bottom: 4px;
}
.holiday {
  color: #ff4d4f;
  font-weight: 400;
  margin-left: 4px;
}
.cal-time {
  color: var(--app-text-2);
}
.cal-leave {
  color: #1677ff;
}
.cal-leave.pending {
  color: #fa8c16;
}
.cal-absent {
  color: #ff4d4f;
  font-weight: 600;
}
.tab-head {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}
.slip-item {
  display: flex;
  justify-content: space-between;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
}
.slip-item:hover {
  background: var(--app-hover);
}
.slip-item.active {
  background: var(--app-primary-bg);
}
.net {
  color: #1677ff;
  font-size: 16px;
}
.section-title {
  font-weight: 600;
  margin: 8px 0 12px;
}
.score-table {
  margin-top: 12px;
}
.full {
  width: 100%;
}
@media (max-width: 767px) {
  .cal-cell {
    min-height: 64px;
  }
}
</style>
