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
            <SelfCalendarTab
              :period="period"
              :current-period="currentPeriod"
              :calendar="calendar"
              :summary="summary"
              :today-str="todayStr"
              :leading="leading"
              @shift="shiftMonth"
            />
          </a-tab-pane>

          <a-tab-pane v-if="employee" key="leave" :tab="$t('hr.self.leaveOvertime')">
            <SelfLeaveTab :leaves="leaves" @apply="(kind) => applyRef.open(kind)" @changed="afterLeaveChange" />
          </a-tab-pane>

          <a-tab-pane v-if="employee" key="payslip" :tab="$t('hr.self.payslip')">
            <SelfPayslipTab :payslips="payslips" />
          </a-tab-pane>

          <a-tab-pane v-if="employee" key="training" :tab="$t('hr.self.myTraining')">
            <SelfTrainingTab :trainings="trainings" />
          </a-tab-pane>

          <a-tab-pane key="perf" :tab="$t('hr.self.perf')">
            <SelfPerfTab :employee="!!employee" :my-appraisals="myAppraisals" :reviews="reviews" @changed="loadPerf" />
          </a-tab-pane>
        </a-tabs>
      </a-card>
    </a-spin>

    <!-- 请假 / 加班申请 -->
    <SelfApplyModal ref="applyRef" :profile="profile" :annual-left="profile ? annualLeft : 0" @success="afterLeaveChange" />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import DictTag from '@/components/DictTag.vue'
import { timeText } from '@/components/hr/hr'
import {
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
import { t } from '@/locales'
import SelfApplyModal from './SelfApplyModal.vue'
import SelfCalendarTab from './SelfCalendarTab.vue'
import SelfLeaveTab from './SelfLeaveTab.vue'
import SelfPayslipTab from './SelfPayslipTab.vue'
import SelfPerfTab from './SelfPerfTab.vue'
import SelfTrainingTab from './SelfTrainingTab.vue'

useDict('hr_emp_status', 'hr_attendance_status', 'hr_leave_type', 'hr_leave_kind', 'hr_leave_status', 'hr_payroll_status',
  'hr_perf_grade', 'hr_appraisal_status', 'hr_training_category', 'hr_training_status')

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
const applyRef = ref()
async function loadLeaves() {
  leaves.value = await selfLeaves()
}
/** 申请、撤销后刷新单据、日历和假期余额 */
function afterLeaveChange() {
  loadLeaves()
  loadCalendar()
  loadProfile()
}

// 工资条、培训
const payslips = ref([])
const trainings = ref([])

// 绩效
const myAppraisals = ref([])
const reviews = ref([])
async function loadPerf() {
  const tasks = [selfReviews({ current: 1, size: 100 })]
  if (employee.value) tasks.push(selfAppraisals({ current: 1, size: 50 }))
  const [r, mine] = await Promise.all(tasks)
  reviews.value = r.records
  myAppraisals.value = mine ? mine.records : []
  if (profile.value) loadProfile()
}

onMounted(async () => {
  loading.value = true
  try {
    await loadProfile()
    if (employee.value) {
      const [l, p, t] = await Promise.all([selfLeaves(), selfPayslips(), selfTrainings(), loadCalendar()])
      leaves.value = l
      payslips.value = p
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
</style>
