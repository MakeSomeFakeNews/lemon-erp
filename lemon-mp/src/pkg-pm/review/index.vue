<template>
  <view>
    <l-segmented v-model="status" :options="tabs" @change="reload" />
    <view v-for="r in paged.list" :key="r.id" class="l-item rv__item" @click="toggle(r)">
      <view v-if="canSelect(r)" class="rv__check" :class="{ 'rv__check--on': selected.includes(r.id) }">{{ selected.includes(r.id) ? '✓' : '' }}</view>
      <view class="rv__body">
        <view class="l-item__head">
          <text class="l-item__title">{{ r.userName }} · {{ t('pm.hoursN', { n: hours(r.hours) }) }}</text>
          <l-tag :text="dictText(dict.pm_timesheet_status, r.status)" :color="dictColor(dict.pm_timesheet_status, r.status)" />
        </view>
        <view class="l-item__desc">{{ r.workDate }} · {{ r.projectName }}<text v-if="r.taskName"> / {{ r.taskName }}</text></view>
        <view v-if="r.content" class="l-item__desc rv__content">{{ r.content }}</view>
        <view v-if="r.status === TimesheetStatus.REJECTED && r.rejectReason" class="l-item__desc rv__reject">
          {{ t('pm.rejectReasonN', { reason: r.rejectReason }) }}
        </view>
        <view v-if="r.approveByName" class="l-item__desc">{{ t('pm.approvedBy', { name: r.approveByName }) }}</view>
      </view>
    </view>
    <l-empty v-if="paged.loaded && !paged.list.length" />
    <l-load-more v-else-if="paged.list.length" :status="paged.status" />

    <template v-if="canApprove && status === TimesheetStatus.SUBMITTED">
      <view class="l-footer-space" />
      <view class="l-footer rv__footer">
        <view class="rv__sum" @click="toggleAll">
          <view class="rv__check" :class="{ 'rv__check--on': allSelected }">{{ allSelected ? '✓' : '' }}</view>
          <text>{{ t('pm.selected', { n: selected.length, hours: hours(selectedHours) }) }}</text>
        </view>
        <button class="l-btn l-btn--default rv__btn" :disabled="!selected.length" @click="reject">{{ t('pm.reject') }}</button>
        <button class="l-btn rv__btn" :disabled="!selected.length" @click="approve">{{ t('pm.approve') }}</button>
      </view>
    </template>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { TimesheetStatus } from '@web/constants/enums'
import { pageTimesheet, reviewTimesheet } from '@/api/pm'
import { hasPerm } from '@/store/user'
import { dictColor, dictText, useDict } from '@/store/dict'
import { usePaged } from '@/composables/usePaged'
import { useTitle } from '@/composables/useTitle'
import { ensureLogin } from '@/utils/session'
import { hours } from '../pm'

const { t } = useI18n()
useTitle('pm.review')
const dict = useDict('pm_timesheet_status')
const status = ref(TimesheetStatus.SUBMITTED)
const tabs = computed(() =>
  [TimesheetStatus.SUBMITTED, TimesheetStatus.APPROVED, TimesheetStatus.REJECTED].map((v) => ({
    value: v,
    label: dictText(dict.pm_timesheet_status, v) || v
  }))
)
const canApprove = computed(() => hasPerm('pm:timesheet:approve'))
const selected = ref([])
const paged = usePaged((p) => pageTimesheet({ ...p, status: status.value }))

const canSelect = (r) => canApprove.value && r.status === TimesheetStatus.SUBMITTED
const selectable = computed(() => paged.list.filter(canSelect))
const allSelected = computed(() => selectable.value.length > 0 && selectable.value.every((r) => selected.value.includes(r.id)))
const selectedHours = computed(() => paged.list.filter((r) => selected.value.includes(r.id)).reduce((s, r) => s + Number(r.hours), 0))

function reload() {
  selected.value = []
  paged.refresh()
}

onShow(async () => {
  if (await ensureLogin()) reload()
})
onPullDownRefresh(reload)
onReachBottom(() => paged.more())

function toggle(r) {
  if (!canSelect(r)) return
  const i = selected.value.indexOf(r.id)
  if (i >= 0) selected.value.splice(i, 1)
  else selected.value.push(r.id)
}

function toggleAll() {
  selected.value = allSelected.value ? [] : selectable.value.map((r) => r.id)
}

async function submit(approved, reason) {
  uni.showLoading({ title: t('common.processing'), mask: true })
  let n
  try {
    n = await reviewTimesheet({ ids: selected.value, approved, reason })
  } catch {
    return
  } finally {
    uni.hideLoading()
  }
  uni.showToast({ title: t(approved ? 'pm.approvedN' : 'pm.rejectedN', { n }), icon: 'none' })
  reload()
}

function approve() {
  uni.showModal({
    title: t('common.tip'),
    content: t('pm.confirmApprove', { n: selected.value.length }),
    confirmText: t('common.ok'),
    cancelText: t('common.cancel'),
    success: (r) => r.confirm && submit(true)
  })
}

function reject() {
  uni.showModal({
    title: t('pm.rejectTitle'),
    editable: true,
    placeholderText: t('pm.rejectReasonPh'),
    confirmText: t('common.ok'),
    cancelText: t('common.cancel'),
    success: (r) => {
      if (!r.confirm) return
      const reason = (r.content || '').trim()
      if (!reason) return uni.showToast({ title: t('pm.rejectReasonPh'), icon: 'none' })
      if (reason.length > 255) return uni.showToast({ title: t('pm.reasonTooLong'), icon: 'none' })
      submit(false, reason)
    }
  })
}
</script>

<style lang="scss">
.rv {
  &__item {
    display: flex;
    gap: 20rpx;
  }
  &__body {
    flex: 1;
    min-width: 0;
  }
  &__check {
    flex-shrink: 0;
    width: 40rpx;
    height: 40rpx;
    margin-top: 4rpx;
    border-radius: 50%;
    border: 2rpx solid #d9d9d9;
    font-size: 24rpx;
    line-height: 40rpx;
    text-align: center;
    color: #fff;
    &--on {
      border-color: $l-primary;
      background: $l-primary;
    }
  }
  &__content {
    color: $l-text-2;
    font-size: 26rpx;
  }
  &__reject {
    color: $l-danger;
  }
  &__footer {
    align-items: center;
  }
  &__sum {
    flex: 1;
    display: flex;
    align-items: center;
    gap: 12rpx;
    font-size: 24rpx;
    color: $l-text-2;
  }
  &__btn {
    flex: none !important;
    width: 180rpx;
    margin: 0;
  }
}
</style>
