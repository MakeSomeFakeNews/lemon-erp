<template>
  <view>
    <l-segmented v-if="hasEmployee" v-model="tab" :options="tabs" @change="current.refresh()" />
    <view v-for="a in current.list" :key="a.id" class="l-item" @click="open(a)">
      <view class="l-item__head">
        <text class="l-item__title">{{ a.planName }}</text>
        <l-tag :text="dictText(dict.hr_appraisal_status, a.status)" :color="dictColor(dict.hr_appraisal_status, a.status)" />
      </view>
      <view v-if="tab === 'reviews'" class="l-item__desc">{{ t('hr.perf.appraiseeOf', { name: [a.empNo, a.employeeName].filter(Boolean).join(' ') }) }} · {{ a.deptName || '-' }}</view>
      <view v-else class="l-item__desc">{{ t('hr.perf.reviewerOf', { name: a.reviewerName || '-' }) }}</view>
      <view class="l-item__desc appr__scores">
        <text>{{ t('hr.perf.selfScore') }} {{ a.selfScore ?? '-' }}</text>
        <text>{{ t('hr.perf.score') }} {{ a.score ?? '-' }}</text>
        <l-tag v-if="a.grade" :text="dictText(dict.hr_perf_grade, a.grade)" :color="dictColor(dict.hr_perf_grade, a.grade)" />
      </view>
      <view v-if="actionOf(a)" class="appr__ops">
        <text class="appr__op">{{ t('hr.perf.' + actionOf(a)) }}</text>
      </view>
    </view>
    <template v-if="current.loaded && !current.list.length">
      <l-empty :text="tab === 'reviews' ? t('hr.perf.noReviews') : ''" />
    </template>
    <l-load-more v-else-if="current.list.length" :status="current.status" />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { AppraisalStatus, PerfStatus } from '@web/constants/enums'
import { selfAppraisals, selfProfile, selfReviews } from '@/api/hr'
import { usePaged } from '@/composables/usePaged'
import { useTitle } from '@/composables/useTitle'
import { dictColor, dictText, useDict } from '@/store/dict'

const { t } = useI18n()
useTitle('hr.title.appraisal')
const dict = useDict('hr_appraisal_status', 'hr_perf_grade')
/** 没有关联员工档案的账号（如只做评分的部门负责人）只有「待我评分」 */
const hasEmployee = ref(false)
const tab = ref('reviews')
const tabs = computed(() => [
  { label: t('hr.perf.mine'), value: 'mine', badge: pendingSelf.value },
  { label: t('hr.perf.reviews'), value: 'reviews', badge: pendingReviews.value }
])
const pendingSelf = ref(0)
const pendingReviews = ref(0)
const mine = usePaged((p) => selfAppraisals(p))
const reviews = usePaged((p) => selfReviews(p))
const current = computed(() => (tab.value === 'mine' ? mine : reviews))
let first = true

onShow(async () => {
  try {
    const p = await selfProfile()
    hasEmployee.value = !!p.employee
    pendingSelf.value = Number(p.pendingSelf || 0)
    pendingReviews.value = Number(p.pendingReviews || 0)
    // 首次进入：有员工档案且没有待评分的，默认看我的考核
    if (first && hasEmployee.value && !pendingReviews.value) tab.value = 'mine'
    first = false
  } catch {
    // 错误已提示
  }
  current.value.refresh()
})
onPullDownRefresh(() => current.value.refresh())
onReachBottom(() => current.value.more())

/** 列表上的操作：自评 / 评分（考核进行中且未完成）/ 查看结果 */
function actionOf(a) {
  if (tab.value === 'mine') {
    if (a.status === AppraisalStatus.SELF && a.planStatus === PerfStatus.SCORING) return 'doSelf'
    if (a.status === AppraisalStatus.DONE) return 'view'
    return null
  }
  if (a.status !== AppraisalStatus.DONE && a.planStatus === PerfStatus.SCORING) return 'doReview'
  return a.status === AppraisalStatus.DONE ? 'view' : null
}

function open(a) {
  const action = actionOf(a)
  const mode = action === 'doSelf' ? 'self' : action === 'doReview' ? 'review' : 'view'
  uni.navigateTo({ url: `/pkg-hr/appraisal/score?id=${a.id}&mode=${mode}` })
}
</script>

<style lang="scss">
.appr {
  &__scores {
    display: flex;
    align-items: center;
    gap: 24rpx;
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
    color: $l-primary;
  }
}
</style>
