<template>
  <view>
    <NoEmployee v-if="profile && !profile.employee" />
    <template v-else-if="profile">
      <view v-for="l in list" :key="l.id" class="l-item">
        <view class="l-item__head">
          <text class="l-item__title">{{ kindLabel(l) }}</text>
          <l-tag :text="dictText(dict.hr_leave_status, l.status)" :color="dictColor(dict.hr_leave_status, l.status)" />
        </view>
        <view class="l-item__desc">{{ t('hr.leave.timeRange', { start: shortDateTime(l.startTime), end: shortDateTime(l.endTime) }) }}</view>
        <view class="l-item__desc">{{ l.leaveNo }} · {{ t('hr.leave.hours', { n: Number(l.hours) }) }}</view>
        <view v-if="l.reason" class="l-item__desc leave__reason">{{ l.reason }}</view>
        <view v-if="l.approveComment" class="l-item__desc">{{ t('hr.leave.approveComment', { text: l.approveComment }) }}</view>
        <view v-if="canCancel(l)" class="leave__ops">
          <text class="leave__op" @click="cancel(l)">{{ t('hr.leave.cancel') }}</text>
        </view>
      </view>
      <l-empty v-if="loaded && !list.length" />
      <view class="l-footer-space" />
      <view class="l-footer">
        <button class="l-btn" @click="apply(LeaveKind.LEAVE)">{{ t('hr.leave.applyLeave') }}</button>
        <button class="l-btn l-btn--ghost" @click="apply(LeaveKind.OVERTIME)">{{ t('hr.leave.applyOvertime') }}</button>
      </view>
    </template>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { LeaveKind, LeaveStatus, OvertimeComp } from '@web/constants/enums'
import { cancelLeave, selfLeaves, selfProfile } from '@/api/hr'
import { runAction } from '@/composables/useAction'
import { useTitle } from '@/composables/useTitle'
import { dictColor, dictText, useDict } from '@/store/dict'
import NoEmployee from '../components/NoEmployee.vue'
import { shortDateTime } from '../hr'

const { t } = useI18n()
useTitle('hr.title.leave')
const dict = useDict('hr_leave_type', 'hr_leave_status')
const profile = ref(null)
const list = ref([])
const loaded = ref(false)

async function load() {
  try {
    profile.value = await selfProfile()
    if (profile.value.employee) list.value = (await selfLeaves()) || []
  } catch {
    // 错误已提示
  } finally {
    loaded.value = true
    uni.stopPullDownRefresh()
  }
}

// 从申请页返回后刷新
onShow(load)
onPullDownRefresh(load)

function kindLabel(l) {
  if (l.kind === LeaveKind.OVERTIME) return l.type === OvertimeComp.TO_LEAVE ? t('hr.leave.overtimeToLeave') : t('hr.leave.overtime')
  return dictText(dict.hr_leave_type, l.type)
}

/** 审批中的可以撤销；已通过的在开始之前可以撤销 */
function canCancel(l) {
  return l.status === LeaveStatus.APPROVING || (l.status === LeaveStatus.APPROVED && new Date(l.startTime.replace(/-/g, '/')) > new Date())
}

function cancel(l) {
  runAction(() => cancelLeave(l.id), { confirm: t('hr.leave.confirmCancel'), success: t('hr.leave.cancelled'), then: load })
}

function apply(kind) {
  uni.navigateTo({ url: `/pkg-hr/leave/apply?kind=${kind}` })
}
</script>

<style lang="scss">
.leave {
  &__reason {
    color: $l-text-2;
  }
  &__ops {
    display: flex;
    justify-content: flex-end;
    margin-top: 16rpx;
    padding-top: 16rpx;
    border-top: 1rpx solid $l-border;
  }
  &__op {
    font-size: 26rpx;
    color: $l-danger;
  }
}
</style>
