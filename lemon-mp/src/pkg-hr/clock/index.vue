<template>
  <view>
    <NoEmployee v-if="profile && !profile.employee" />
    <template v-else-if="profile">
      <view class="clock">
        <view class="clock__name">{{ profile.employee.name }} · {{ profile.employee.empNo }}</view>
        <view class="clock__time">{{ nowTime }}</view>
        <view class="clock__date">{{ nowDate }} {{ t('hr.week.' + nowWeek) }}</view>
        <view class="clock__btn" :class="{ 'clock__btn--out': record?.checkIn, 'clock__btn--busy': busy }" @click="clock">
          <text class="clock__btn-text">{{ record?.checkIn ? t('hr.clock.clockOut') : t('hr.clock.clockIn') }}</text>
          <text class="clock__btn-time">{{ nowTime.slice(0, 5) }}</text>
        </view>
        <view class="clock__rule">{{ t('hr.clock.rule') }}</view>
      </view>

      <l-card>
        <view class="clock__records">
          <view class="clock__record">
            <view class="l-muted">{{ t('hr.clock.checkIn') }}</view>
            <view class="clock__record-time">{{ record?.checkIn ? timeText(record.checkIn) : t('hr.clock.notYet') }}</view>
          </view>
          <view class="clock__record">
            <view class="l-muted">{{ t('hr.clock.checkOut') }}</view>
            <view class="clock__record-time">{{ record?.checkOut ? timeText(record.checkOut) : t('hr.clock.notYet') }}</view>
          </view>
        </view>
        <view v-if="record" class="clock__status">
          <l-tag :text="dictText(dict.hr_attendance_status, record.status)" :color="dictColor(dict.hr_attendance_status, record.status)" />
          <text v-if="record.workHours" class="l-muted">{{ t('hr.clock.workHours', { n: Number(record.workHours) }) }}</text>
        </view>
      </l-card>

      <l-card>
        <l-cell :label="t('hr.clock.viewAttendance')" arrow @click="go('/pkg-hr/attendance/index')" />
      </l-card>
    </template>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onHide, onShow, onUnload } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { selfCalendar, selfClock, selfProfile } from '@/api/hr'
import { useTitle } from '@/composables/useTitle'
import { vibrate } from '@/composables/useAction'
import { dictColor, dictText, useDict } from '@/store/dict'
import { period, today } from '@/utils/format'
import NoEmployee from '../components/NoEmployee.vue'
import { timeText } from '../hr'

const { t } = useI18n()
useTitle('hr.title.clock')
const dict = useDict('hr_attendance_status')
const profile = ref(null)
const record = ref(null)
const busy = ref(false)
const now = ref(new Date())
let timer = null

const pad = (n) => String(n).padStart(2, '0')
const nowTime = computed(() => `${pad(now.value.getHours())}:${pad(now.value.getMinutes())}:${pad(now.value.getSeconds())}`)
const nowDate = computed(() => `${now.value.getFullYear()}-${pad(now.value.getMonth() + 1)}-${pad(now.value.getDate())}`)
const nowWeek = computed(() => now.value.getDay())

async function load() {
  profile.value = await selfProfile()
  if (!profile.value.employee) return
  const days = await selfCalendar(period())
  record.value = (days || []).find((d) => d.date === today())?.attendance || null
}

onShow(() => {
  now.value = new Date()
  timer = setInterval(() => (now.value = new Date()), 1000)
  load().catch(() => {})
})
const stop = () => clearInterval(timer)
onHide(stop)
onUnload(stop)

async function clock() {
  if (busy.value) return
  busy.value = true
  try {
    const r = await selfClock()
    record.value = r
    vibrate(true)
    uni.showToast({ title: t(r.checkOut ? 'hr.clock.clockOutSuccess' : 'hr.clock.clockInSuccess', { time: timeText(r.checkOut || r.checkIn) }), icon: 'none' })
  } catch {
    vibrate(false)
  } finally {
    busy.value = false
  }
}

function go(url) {
  uni.navigateTo({ url })
}
</script>

<style lang="scss">
.clock {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48rpx 24rpx 16rpx;
  &__name {
    font-size: 26rpx;
    color: $l-text-3;
  }
  &__time {
    margin-top: 16rpx;
    font-size: 72rpx;
    font-weight: 600;
    font-variant-numeric: tabular-nums;
  }
  &__date {
    font-size: 26rpx;
    color: $l-text-2;
  }
  &__btn {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 320rpx;
    height: 320rpx;
    margin-top: 56rpx;
    border-radius: 50%;
    color: #fff;
    background: linear-gradient(135deg, #4096ff, $l-primary);
    box-shadow: 0 16rpx 40rpx rgba(22, 119, 255, 0.35);
    &--out {
      background: linear-gradient(135deg, #36cfc9, #13a8a8);
      box-shadow: 0 16rpx 40rpx rgba(19, 168, 168, 0.35);
    }
    &--busy {
      opacity: 0.6;
    }
  }
  &__btn-text {
    font-size: 36rpx;
    font-weight: 600;
  }
  &__btn-time {
    margin-top: 8rpx;
    font-size: 28rpx;
    opacity: 0.9;
  }
  &__rule {
    margin-top: 32rpx;
    padding: 0 40rpx;
    font-size: 22rpx;
    color: $l-text-3;
    text-align: center;
  }
  &__records {
    display: flex;
  }
  &__record {
    flex: 1;
    text-align: center;
    font-size: 24rpx;
  }
  &__record-time {
    margin-top: 8rpx;
    font-size: 36rpx;
    font-weight: 600;
  }
  &__status {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 16rpx;
    margin-top: 20rpx;
    font-size: 24rpx;
  }
}
</style>
