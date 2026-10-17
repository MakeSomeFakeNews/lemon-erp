<template>
  <div class="cal-head">
    <a-button size="small" @click="emit('shift', -1)"><LeftOutlined /></a-button>
    <b>{{ periodText(period) }}</b>
    <a-button size="small" :disabled="period >= currentPeriod" @click="emit('shift', 1)"><RightOutlined /></a-button>
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
        <DictTag v-if="d.attendance.status !== AttendanceStatus.NORMAL" type="hr_attendance_status" :value="d.attendance.status" />
      </template>
      <div v-for="l in d.leaves" :key="l.id" class="cal-leave" :class="{ pending: l.status === LeaveStatus.APPROVING }">
        {{ l.kind === LeaveKind.OVERTIME ? (l.type === OvertimeComp.TO_LEAVE ? $t('hr.self.overtimeToLeave') : $t('hr.self.overtime')) : dictLabel(dict.hr_leave_type, l.type) }} {{ Number(l.hours) }}h
      </div>
      <div v-if="d.absent" class="cal-absent">{{ $t('hr.self.absentLabel') }}</div>
    </div>
  </div>
</template>

<script setup>
import DictTag from '@/components/DictTag.vue'
import { LeftOutlined, RightOutlined } from '@ant-design/icons-vue'
import { periodText, timeText } from '@/components/hr/hr'
import { useDict } from '@/composables/useDict'
import { findDict } from '@/utils/dict'
import { t } from '@/locales'
import { AttendanceStatus, LeaveKind, LeaveStatus, OvertimeComp } from '@/constants/enums'

/** 员工自助 - 考勤日历：period 为 yyyyMM，切换月份触发 shift(±1) */
defineProps({
  period: { type: Number, required: true },
  currentPeriod: { type: Number, required: true },
  calendar: { type: Array, default: () => [] },
  summary: { type: Object, default: undefined },
  todayStr: { type: String, required: true },
  leading: { type: Number, default: 0 }
})
const emit = defineEmits(['shift'])
const dict = useDict('hr_leave_type')
const dictLabel = (list, v) => findDict(list, v)?.label || ''
const WEEK = [0, 1, 2, 3, 4, 5, 6].map((i) => t(`hr.self.week.${i}`))
</script>

<style scoped>
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
@media (max-width: 767px) {
  .cal-cell {
    min-height: 64px;
  }
}
</style>
