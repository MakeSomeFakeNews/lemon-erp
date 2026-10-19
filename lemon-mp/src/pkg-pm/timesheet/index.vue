<template>
  <view>
    <view class="ts__week">
      <text class="ts__nav" @click="shift(-7)">‹</text>
      <view class="ts__range">
        <view>{{ weekStart.slice(5) }} ~ {{ weekEnd.slice(5) }}</view>
        <view class="ts__total">{{ t('pm.weekTotal', { n: hours(weekTotal) }) }}</view>
      </view>
      <text class="ts__nav" :class="{ 'ts__nav--off': isThisWeek }" @click="!isThisWeek && shift(7)">›</text>
    </view>

    <view class="ts__days">
      <view
        v-for="d in days"
        :key="d.date"
        class="ts__day"
        :class="{ 'ts__day--active': d.date === selected, 'ts__day--today': d.date === todayStr }"
        @click="selected = d.date"
      >
        <text class="ts__day-name">{{ t('pm.week.' + d.key) }}</text>
        <text class="ts__day-date">{{ d.date.slice(8) }}</text>
        <text class="ts__day-hours" :class="{ 'ts__day-hours--over': d.hours > 8 }">{{ d.hours ? hours(d.hours) + 'h' : '-' }}</text>
      </view>
    </view>

    <view v-if="error" class="ts__error">{{ error }}</view>
    <view v-for="r in dayList" :key="r.id" class="l-item" @click="open(r)">
      <view class="l-item__head">
        <text class="l-item__title">{{ r.projectName }}</text>
        <l-tag :text="dictText(dict.pm_timesheet_status, r.status)" :color="dictColor(dict.pm_timesheet_status, r.status)" />
      </view>
      <view class="l-item__desc">
        <text v-if="r.taskName">{{ r.taskName }} · </text>
        <text class="ts__hours">{{ t('pm.hoursN', { n: hours(r.hours) }) }}</text>
      </view>
      <view v-if="r.content" class="l-item__desc ts__content">{{ r.content }}</view>
      <view v-if="r.status === TimesheetStatus.REJECTED && r.rejectReason" class="l-item__desc ts__reject">
        {{ t('pm.rejectReasonN', { reason: r.rejectReason }) }}
      </view>
      <view v-if="r.status !== TimesheetStatus.APPROVED" class="ts__actions">
        <text class="ts__action" @click.stop="open(r)">{{ t('common.edit') }}</text>
        <text class="ts__action ts__action--danger" @click.stop="remove(r)">{{ t('common.delete') }}</text>
      </view>
    </view>
    <l-empty v-if="loaded && !dayList.length && !error" :text="t('pm.noTimesheet')" />

    <view class="l-footer-space" />
    <view class="l-footer">
      <button class="l-btn" :disabled="selected > todayStr" @click="add">{{ t('pm.log') }}</button>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { TimesheetStatus } from '@web/constants/enums'
import { deleteTimesheet, myTimesheet } from '@/api/pm'
import { dictColor, dictText, useDict } from '@/store/dict'
import { runAction } from '@/composables/useAction'
import { useTitle } from '@/composables/useTitle'
import { ensureLogin } from '@/utils/session'
import { today } from '@/utils/format'
import { addDays, hours, monday } from '../pm'

const { t } = useI18n()
useTitle('pm.mine')
const dict = useDict('pm_timesheet_status')
const DAY_KEYS = ['mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun']

const todayStr = today()
const weekStart = ref(monday(todayStr))
const weekEnd = computed(() => addDays(weekStart.value, 6))
const selected = ref(todayStr)
const list = ref([])
const loaded = ref(false)
const error = ref('')

const isThisWeek = computed(() => weekStart.value === monday(todayStr))
// 驳回的工时不计入合计（与 Web 端一致）
const counted = (r) => r.status !== TimesheetStatus.REJECTED
const weekTotal = computed(() => list.value.filter(counted).reduce((s, r) => s + Number(r.hours), 0))
const days = computed(() =>
  DAY_KEYS.map((key, i) => {
    const date = addDays(weekStart.value, i)
    const h = list.value.filter((r) => r.workDate === date && counted(r)).reduce((s, r) => s + Number(r.hours), 0)
    return { key, date, hours: h }
  })
)
const dayList = computed(() => list.value.filter((r) => r.workDate === selected.value))

async function load() {
  try {
    list.value = (await myTimesheet(weekStart.value, weekEnd.value)) || []
    error.value = ''
  } catch (e) {
    list.value = []
    error.value = e?.message || ''
  } finally {
    loaded.value = true
  }
}

function shift(n) {
  weekStart.value = addDays(weekStart.value, n)
  // 切换周后选中同一个星期几，不超过今天
  const d = addDays(selected.value, n)
  selected.value = d > todayStr ? todayStr : d
  load()
}

onShow(async () => {
  if (await ensureLogin()) load()
})

function add() {
  uni.navigateTo({ url: `/pkg-pm/timesheet/edit?date=${selected.value}` })
}

function open(r) {
  if (r.status === TimesheetStatus.APPROVED) return
  uni.navigateTo({ url: `/pkg-pm/timesheet/edit?id=${r.id}&date=${r.workDate}` })
}

function remove(r) {
  runAction(() => deleteTimesheet(r.id), { confirm: t('pm.confirmDelete'), success: t('pm.deleted'), then: load })
}
</script>

<style lang="scss">
.ts {
  &__week {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 24rpx 32rpx 8rpx;
  }
  &__nav {
    padding: 0 24rpx;
    font-size: 48rpx;
    color: $l-primary;
    &--off {
      color: #d9d9d9;
    }
  }
  &__range {
    text-align: center;
    font-size: 32rpx;
    font-weight: 600;
  }
  &__total {
    margin-top: 4rpx;
    font-size: 24rpx;
    font-weight: normal;
    color: $l-text-3;
  }
  &__days {
    display: flex;
    gap: 8rpx;
    margin: 16rpx 24rpx 4rpx;
  }
  &__day {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 14rpx 0;
    background: #fff;
    border-radius: 12rpx;
    border: 2rpx solid transparent;
    &--today .ts__day-date {
      color: $l-primary;
    }
    &--active {
      border-color: $l-primary;
      background: #e6f4ff;
    }
  }
  &__day-name {
    font-size: 22rpx;
    color: $l-text-3;
  }
  &__day-date {
    margin: 4rpx 0;
    font-size: 30rpx;
    font-weight: 600;
  }
  &__day-hours {
    font-size: 22rpx;
    color: $l-text-2;
    &--over {
      color: $l-danger;
    }
  }
  &__error {
    margin: 24rpx;
    padding: 20rpx 24rpx;
    border-radius: $l-radius;
    font-size: 26rpx;
    color: #cf1322;
    background: #fff1f0;
  }
  &__hours {
    color: $l-text;
    font-weight: 600;
  }
  &__content {
    color: $l-text-2;
    font-size: 26rpx;
  }
  &__reject {
    color: $l-danger;
  }
  &__actions {
    display: flex;
    justify-content: flex-end;
    gap: 40rpx;
    margin-top: 16rpx;
    padding-top: 16rpx;
    border-top: 1rpx solid $l-border;
  }
  &__action {
    font-size: 26rpx;
    color: $l-primary;
    &--danger {
      color: $l-danger;
    }
  }
}
</style>
