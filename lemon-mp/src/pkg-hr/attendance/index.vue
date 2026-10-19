<template>
  <view>
    <NoEmployee v-if="profile && !profile.employee" />
    <template v-else-if="profile">
      <view class="att__month">
        <text class="att__arrow" @click="shift(-1)">‹</text>
        <text class="att__month-text">{{ periodText(current) }}</text>
        <text class="att__arrow" :class="{ 'att__arrow--disabled': current >= thisPeriod }" @click="shift(1)">›</text>
      </view>

      <l-card>
        <view class="att__week">
          <text v-for="i in 7" :key="i" class="att__week-day">{{ t('hr.week.' + (i - 1)) }}</text>
        </view>
        <view class="att__grid">
          <view v-for="n in leading" :key="'b' + n" class="att__cell" />
          <view
            v-for="d in days"
            :key="d.date"
            class="att__cell"
            :class="{ 'att__cell--rest': !d.workday, 'att__cell--today': d.date === todayStr, 'att__cell--active': d.date === selected }"
            @click="selected = d.date"
          >
            <text class="att__day">{{ Number(d.date.slice(8)) }}</text>
            <text v-if="d.holidayName" class="att__holiday">{{ d.holidayName }}</text>
            <view class="att__dots">
              <view v-if="d.absent" class="att__dot att__dot--absent" />
              <view v-else-if="d.attendance" class="att__dot" :class="d.attendance.status === AttendanceStatus.NORMAL ? 'att__dot--normal' : 'att__dot--abnormal'" />
              <view v-if="d.leaves && d.leaves.length" class="att__dot att__dot--leave" />
            </view>
          </view>
        </view>
        <view class="att__legend">
          <view class="att__legend-item"><view class="att__dot att__dot--normal" />{{ t('hr.attendance.legendNormal') }}</view>
          <view class="att__legend-item"><view class="att__dot att__dot--abnormal" />{{ t('hr.attendance.legendAbnormal') }}</view>
          <view class="att__legend-item"><view class="att__dot att__dot--absent" />{{ t('hr.attendance.legendAbsent') }}</view>
          <view class="att__legend-item"><view class="att__dot att__dot--leave" />{{ t('hr.attendance.legendLeave') }}</view>
        </view>
      </l-card>

      <l-card v-if="selectedDay" :title="t('hr.attendance.dayDetail', { date: selectedDay.date })">
        <template #extra>
          <text v-if="selectedDay.holidayName || !selectedDay.workday">{{ selectedDay.holidayName || t('hr.attendance.restDay') }}</text>
        </template>
        <template v-if="selectedDay.attendance">
          <l-cell :label="t('hr.clock.checkIn')" :value="timeText(selectedDay.attendance.checkIn)" />
          <l-cell :label="t('hr.clock.checkOut')" :value="timeText(selectedDay.attendance.checkOut)" />
          <l-cell :label="t('hr.attendance.workHours')" :value="t('hr.attendance.hoursUnit', { n: Number(selectedDay.attendance.workHours || 0) })" />
          <l-cell :label="t('common.status')">
            <l-tag
              :text="dictText(dict.hr_attendance_status, selectedDay.attendance.status)"
              :color="dictColor(dict.hr_attendance_status, selectedDay.attendance.status)"
            />
          </l-cell>
        </template>
        <view v-else-if="!selectedDay.leaves?.length" class="l-muted att__none">
          {{ selectedDay.absent ? t('hr.attendance.absent') : t('hr.attendance.noRecord') }}
        </view>
        <l-cell v-for="l in selectedDay.leaves" :key="l.id" :label="leaveLabel(l)">
          <text>{{ t('hr.leave.hours', { n: Number(l.hours) }) }} </text>
          <l-tag :text="dictText(dict.hr_leave_status, l.status)" :color="dictColor(dict.hr_leave_status, l.status)" />
        </l-cell>
      </l-card>

      <l-card v-if="summary">
        <view class="att__stats">
          <view class="att__stat">
            <view class="att__stat-value">{{ summary.shouldDays }}</view>
            <view class="att__stat-label">{{ t('hr.attendance.should') }}</view>
          </view>
          <view class="att__stat">
            <view class="att__stat-value">{{ summary.actualDays }}</view>
            <view class="att__stat-label">{{ t('hr.attendance.actual') }}</view>
          </view>
          <view class="att__stat">
            <view class="att__stat-value" :class="{ 'att__warn': summary.lateCount }">{{ summary.lateCount }}</view>
            <view class="att__stat-label">{{ t('hr.attendance.late') }}</view>
          </view>
          <view class="att__stat">
            <view class="att__stat-value" :class="{ 'att__warn': summary.earlyCount }">{{ summary.earlyCount }}</view>
            <view class="att__stat-label">{{ t('hr.attendance.early') }}</view>
          </view>
          <view class="att__stat">
            <view class="att__stat-value" :class="{ 'att__warn': summary.missingCount }">{{ summary.missingCount }}</view>
            <view class="att__stat-label">{{ t('hr.attendance.missing') }}</view>
          </view>
          <view class="att__stat">
            <view class="att__stat-value" :class="{ 'att__danger': summary.absentDays }">{{ summary.absentDays }}</view>
            <view class="att__stat-label">{{ t('hr.attendance.absent') }}</view>
          </view>
          <view class="att__stat">
            <view class="att__stat-value">{{ leaveTotal }}</view>
            <view class="att__stat-label">{{ t('hr.attendance.leaveHours') }}</view>
          </view>
          <view class="att__stat">
            <view class="att__stat-value">{{ Number(summary.overtimeHours || 0) }}</view>
            <view class="att__stat-label">{{ t('hr.attendance.overtime') }}</view>
          </view>
        </view>
      </l-card>

      <l-card :title="t('hr.attendance.balance')">
        <l-cell :label="t('hr.attendance.annualLeft')" :value="t('hr.attendance.daysUnit', { n: annualLeft })" />
        <view class="l-muted att__sub">{{ t('hr.attendance.annualTotal', { total: profile.annualDays, used: Number(profile.annualUsedDays) }) }}</view>
        <l-cell :label="t('hr.attendance.compLeft')" :value="t('hr.attendance.hoursUnit', { n: Number(profile.compensatoryHours || 0) })" />
      </l-card>
    </template>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { AttendanceStatus, LeaveKind, OvertimeComp } from '@web/constants/enums'
import { selfCalendar, selfProfile, selfSummary } from '@/api/hr'
import { useTitle } from '@/composables/useTitle'
import { dictColor, dictText, useDict } from '@/store/dict'
import { period, today } from '@/utils/format'
import NoEmployee from '../components/NoEmployee.vue'
import { periodText, shiftPeriod, timeText } from '../hr'

const { t } = useI18n()
useTitle('hr.title.attendance')
const dict = useDict('hr_attendance_status', 'hr_leave_type', 'hr_leave_status')
const thisPeriod = period()
const todayStr = today()
const current = ref(thisPeriod)
const profile = ref(null)
const days = ref([])
const summary = ref(null)
const selected = ref(todayStr)

/** 月初是星期几，前面补空格 */
const leading = computed(() => (days.value.length ? new Date(days.value[0].date.replace(/-/g, '/')).getDay() : 0))
const selectedDay = computed(() => days.value.find((d) => d.date === selected.value))
const annualLeft = computed(() => Math.max(0, +(profile.value.annualDays - Number(profile.value.annualUsedDays)).toFixed(1)))
const leaveTotal = computed(() => Object.values(summary.value?.leaveHours || {}).reduce((s, v) => s + Number(v || 0), 0))

function leaveLabel(l) {
  if (l.kind === LeaveKind.OVERTIME) return l.type === OvertimeComp.TO_LEAVE ? t('hr.leave.overtimeToLeave') : t('hr.leave.overtime')
  return dictText(dict.hr_leave_type, l.type)
}

async function loadMonth() {
  const p = current.value
  const [cal, sum] = await Promise.all([selfCalendar(p), selfSummary(p)])
  if (p !== current.value) return
  days.value = cal || []
  summary.value = sum
  if (!days.value.some((d) => d.date === selected.value)) selected.value = days.value[0]?.date
}

onLoad(async () => {
  try {
    profile.value = await selfProfile()
    if (profile.value.employee) await loadMonth()
  } catch {
    // 错误已提示
  }
})

function shift(delta) {
  const next = shiftPeriod(current.value, delta)
  if (next > thisPeriod) return
  current.value = next
  selected.value = next === thisPeriod ? todayStr : null
  loadMonth().catch(() => {})
}
</script>

<style lang="scss">
.att {
  &__month {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 48rpx;
    padding: 28rpx 0 0;
  }
  &__month-text {
    font-size: 32rpx;
    font-weight: 600;
  }
  &__arrow {
    padding: 0 24rpx;
    font-size: 44rpx;
    color: $l-primary;
    &--disabled {
      color: #d9d9d9;
    }
  }
  &__week,
  &__grid {
    display: flex;
    flex-wrap: wrap;
  }
  &__week-day {
    width: 14.2857%;
    text-align: center;
    font-size: 24rpx;
    color: $l-text-3;
    padding-bottom: 12rpx;
  }
  &__cell {
    width: 14.2857%;
    height: 104rpx;
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding-top: 10rpx;
    border-radius: 12rpx;
    &--rest .att__day {
      color: $l-text-3;
    }
    &--today .att__day {
      color: $l-primary;
      font-weight: 600;
    }
    &--active {
      background: #e6f4ff;
    }
  }
  &__day {
    font-size: 28rpx;
  }
  &__holiday {
    font-size: 18rpx;
    color: $l-danger;
    max-width: 96rpx;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }
  &__dots {
    display: flex;
    gap: 6rpx;
    margin-top: 6rpx;
  }
  &__dot {
    width: 12rpx;
    height: 12rpx;
    border-radius: 50%;
    &--normal {
      background: $l-success;
    }
    &--abnormal {
      background: $l-warning;
    }
    &--absent {
      background: $l-danger;
    }
    &--leave {
      background: $l-primary;
    }
  }
  &__legend {
    display: flex;
    flex-wrap: wrap;
    gap: 24rpx;
    margin-top: 16rpx;
    font-size: 22rpx;
    color: $l-text-3;
  }
  &__legend-item {
    display: flex;
    align-items: center;
    gap: 8rpx;
  }
  &__none {
    padding: 16rpx 0;
    font-size: 26rpx;
  }
  &__stats {
    display: flex;
    flex-wrap: wrap;
  }
  &__stat {
    width: 25%;
    padding: 12rpx 0;
    text-align: center;
  }
  &__stat-value {
    font-size: 36rpx;
    font-weight: 600;
  }
  &__stat-label {
    margin-top: 4rpx;
    font-size: 22rpx;
    color: $l-text-3;
  }
  &__warn {
    color: $l-warning;
  }
  &__danger {
    color: $l-danger;
  }
  &__sub {
    font-size: 22rpx;
    margin-top: -12rpx;
    padding-bottom: 12rpx;
  }
}
</style>
