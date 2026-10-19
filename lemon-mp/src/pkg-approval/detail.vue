<template>
  <view v-if="detail">
    <l-card>
      <template #title>{{ detail.name }}</template>
      <template #extra>
        <l-tag :text="dictText(dict.wf_status, detail.status)" :color="dictColor(dict.wf_status, detail.status)" />
      </template>
      <l-cell :label="t('approval.starter')" :value="detail.startUserName" />
      <l-cell :label="t('approval.process')" :value="detail.processName" />
      <l-cell :label="t('approval.startTime')" :value="detail.startTime" />
      <l-cell v-if="detail.endTime" :label="t('approval.endTime')" :value="detail.endTime" />
    </l-card>

    <!-- 业务单据发起的流程：发起时的业务信息（金额、天数等），字典值显示为标签 -->
    <l-card v-if="detail.business && detail.business.length" :title="t('approval.business')">
      <l-cell v-for="b in detail.business" :key="b.field" :label="b.label">
        <l-tag v-if="b.dictType" :text="dictText(dict[b.dictType], b.value)" :color="dictColor(dict[b.dictType], b.value)" />
        <text v-else>{{ businessValue(b) }}</text>
      </l-cell>
    </l-card>
    <l-card v-else :title="t('approval.form')">
      <view v-if="!formItems.length" class="l-muted dt__none">{{ t('approval.noForm') }}</view>
      <l-cell v-for="f in formItems" :key="f.key" :label="f.label">
        <text :class="{ dt__multiline: f.multiline }">{{ f.value }}</text>
      </l-cell>
    </l-card>

    <l-card :title="t('approval.records')">
      <view v-for="(r, i) in detail.records" :key="i" class="dt__record">
        <view class="dt__rail">
          <view class="dt__dot" :class="'dt__dot--' + (ACTIONS[r.action] || 'default')" />
          <view v-if="i < detail.records.length - 1" class="dt__line" />
        </view>
        <view class="dt__body">
          <view class="dt__head">
            <text class="dt__node">{{ r.nodeName }}</text>
            <text class="dt__time">{{ r.time ? shortTime(r.time) : '' }}</text>
          </view>
          <view class="dt__user">
            <text>{{ r.userName || '-' }}</text>
            <l-tag :text="actionLabel(r.action)" :color="ACTIONS[r.action]" />
          </view>
          <view v-if="r.comment" class="dt__comment">{{ r.comment }}</view>
        </view>
      </view>
    </l-card>

    <template v-if="canHandle || detail.cancelable">
      <view class="l-footer-space" />
      <view class="l-footer">
        <button v-if="detail.cancelable" class="l-btn l-btn--default" @click="openAction('cancel')">{{ t('approval.action.cancel') }}</button>
        <template v-if="canHandle">
          <button class="l-btn l-btn--default" @click="openAction('transfer')">{{ t('approval.action.transfer') }}</button>
          <button class="l-btn l-btn--danger" @click="openAction('reject')">{{ t('approval.action.reject') }}</button>
          <button class="l-btn" @click="openAction('approve')">{{ t('approval.action.approve') }}</button>
        </template>
      </view>
    </template>

    <!-- 同意 / 拒绝 / 转交 / 撤回：填写意见 -->
    <view v-if="action" class="dt__mask" @click="action = ''">
      <view class="dt__sheet" @click.stop>
        <view class="dt__sheet-title">{{ t('approval.action.' + action) }}</view>
        <l-field v-if="action === 'transfer'" :label="t('approval.action.transferTo')" required>
          <view class="dt__picker" @click="openPicker">
            <text v-if="transferee">{{ transferee.nickname || transferee.username }}</text>
            <text v-else class="l-placeholder">{{ t('approval.action.notSelected') }}</text>
            <text class="dt__arrow">›</text>
          </view>
        </l-field>
        <l-field :label="FORMS[action].label" :required="action === 'reject'">
          <textarea
            v-model="comment"
            class="l-textarea"
            fixed
            maxlength="500"
            :placeholder="FORMS[action].placeholder"
            placeholder-class="l-placeholder"
          />
        </l-field>
        <view class="dt__sheet-btns">
          <button class="l-btn l-btn--default" @click="action = ''">{{ t('common.cancel') }}</button>
          <button class="l-btn" :class="{ 'l-btn--danger': action === 'reject' || action === 'cancel' }" @click="submit">
            {{ t('approval.action.' + action) }}
          </button>
        </view>
      </view>
    </view>

    <!-- 选择转交人 -->
    <view v-if="pickerOpen" class="dt__mask" @click="pickerOpen = false">
      <view class="dt__sheet dt__sheet--tall" @click.stop>
        <view class="dt__sheet-title">{{ t('approval.user.title') }}</view>
        <view class="l-search dt__user-search">
          <input v-model="userKeyword" :placeholder="t('approval.user.searchPh')" placeholder-class="l-placeholder" confirm-type="search" @confirm="users.refresh()" />
        </view>
        <scroll-view scroll-y class="dt__users" @scrolltolower="users.more()">
          <view
            v-for="u in users.list"
            :key="u.id"
            class="dt__user-row"
            :class="{ 'dt__user-row--disabled': u.id === userState.user?.id }"
            @click="pickUser(u)"
          >
            <view class="dt__avatar">{{ (u.nickname || u.username || '?').slice(0, 1) }}</view>
            <view class="dt__user-info">
              <view>{{ u.nickname || u.username }}<text v-if="u.id === userState.user?.id" class="l-muted"> ({{ t('approval.user.self') }})</text></view>
              <view class="l-muted dt__user-sub">{{ [u.username, u.deptName].filter(Boolean).join(' · ') }}</view>
            </view>
          </view>
          <l-empty v-if="users.loaded && !users.list.length" />
          <l-load-more v-else-if="users.list.length" :status="users.status" />
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { ApprovalAction, FormFieldType } from '@web/constants/enums'
import { approveTask, cancelInstance, getInstance, pageIdentityUsers, rejectTask, transferTask } from '@/api/workflow'
import { usePaged } from '@/composables/usePaged'
import { runAction } from '@/composables/useAction'
import { useTitle } from '@/composables/useTitle'
import { requestSubscribe } from '@/utils/subscribe'
import { dictColor, dictText, useDict } from '@/store/dict'
import { hasPerm, userState } from '@/store/user'
import { ensureLogin } from '@/utils/session'
import { shortTime } from '@/utils/format'

/**
 * 流程实例详情：基本信息、业务信息或表单、审批记录；当前用户有待办时可同意 / 拒绝 / 转交，发起人在审批中可撤回
 */
const { t } = useI18n()
useTitle('approval.detailTitle')
const dict = useDict('wf_status')

/** 审批记录动作 → 标签颜色 */
const ACTIONS = {
  [ApprovalAction.START]: 'primary',
  [ApprovalAction.APPROVE]: 'success',
  [ApprovalAction.REJECT]: 'danger',
  [ApprovalAction.TRANSFER]: 'primary',
  [ApprovalAction.CANCEL]: 'default',
  [ApprovalAction.TERMINATE]: 'danger',
  [ApprovalAction.CC]: 'purple',
  [ApprovalAction.PENDING]: 'warning'
}
const actionLabel = (a) => (a ? t('approval.record.' + a.toLowerCase()) : '')

const FORMS = computed(() => ({
  approve: { label: t('approval.action.comment'), placeholder: t('approval.action.commentPh'), done: t('approval.action.approved') },
  reject: { label: t('approval.action.rejectReason'), placeholder: t('approval.action.rejectReasonPh'), done: t('approval.action.rejected') },
  transfer: { label: t('approval.action.transferNote'), placeholder: t('approval.action.commentPh'), done: t('approval.action.transferred') },
  cancel: { label: t('approval.action.cancelReason'), placeholder: t('approval.action.cancelPh'), done: t('approval.action.cancelled') }
}))

const id = ref('')
const detail = ref(null)
const canHandle = computed(() => !!detail.value?.todoTaskId && hasPerm('wf:task:todo'))

onLoad(async (q) => {
  id.value = decodeURIComponent(q.id || '')
  if (await ensureLogin()) load()
})

async function load() {
  uni.showLoading({ mask: true })
  try {
    const data = await getInstance(id.value)
    // 业务变量中的字典（请假类型、凭证来源等）
    const types = [...new Set((data.business || []).map((b) => b.dictType).filter(Boolean))]
    if (types.length) useDict(...types)
    detail.value = data
  } catch {
    // 错误已提示
  } finally {
    uni.hideLoading()
  }
}

const display = (v) => (v === null || v === undefined || v === '' ? '-' : typeof v === 'object' ? JSON.stringify(v) : String(v))

/** 设计器表单按字段显示；没有表单定义时直接显示变量 */
const formItems = computed(() => {
  const d = detail.value
  if (!d) return []
  if (d.form?.length) {
    return d.form.map((f) => ({ key: f.field, label: f.label, value: display(d.formData?.[f.field]), multiline: f.type === FormFieldType.TEXTAREA }))
  }
  return Object.entries(d.formData || {}).map(([k, v]) => ({ key: k, label: k, value: display(v) }))
})

const businessValue = (b) =>
  b.type === FormFieldType.NUMBER && typeof b.value === 'number' ? b.value.toLocaleString('en-US', { maximumFractionDigits: 2 }) : display(b.value)

// 操作
const action = ref('')
const comment = ref('')
const transferee = ref(null)

function openAction(type) {
  action.value = type
  comment.value = ''
  transferee.value = null
}

async function submit() {
  // 审批人处理完一条待办，顺便续订下一次的审批提醒（只能在点击事件中同步申请）
  requestSubscribe()
  const type = action.value
  const text = comment.value.trim() || undefined
  if (type === 'reject' && !text) return toast(t('approval.action.rejectReasonPh'))
  if (type === 'transfer' && !transferee.value) return toast(t('approval.action.selectTransferee'))
  const taskId = detail.value.todoTaskId
  const run = {
    approve: () => approveTask(taskId, text),
    reject: () => rejectTask(taskId, text),
    transfer: () => transferTask(taskId, transferee.value.id, text),
    cancel: () => cancelInstance(detail.value.id, text)
  }[type]
  await runAction(run, {
    success: FORMS.value[type].done,
    then: () => {
      action.value = ''
      // 处理后流程可能已结束或流转给他人，重新加载显示最新进度
      return load()
    }
  })
}

// 选择转交人
const pickerOpen = ref(false)
const userKeyword = ref('')
const users = usePaged((p) => pageIdentityUsers({ ...p, username: userKeyword.value.trim() || undefined }))

function openPicker() {
  pickerOpen.value = true
  if (!users.loaded) users.refresh()
}

function pickUser(u) {
  if (u.id === userState.user?.id) return
  transferee.value = u
  pickerOpen.value = false
}

function toast(title) {
  uni.showToast({ title, icon: 'none' })
}
</script>

<style lang="scss">
.dt {
  &__none {
    font-size: 26rpx;
    padding: 8rpx 0;
  }
  &__multiline {
    white-space: pre-wrap;
    text-align: left;
  }
  &__record {
    display: flex;
  }
  &__rail {
    position: relative;
    width: 40rpx;
    flex-shrink: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
  }
  &__dot {
    width: 18rpx;
    height: 18rpx;
    margin-top: 12rpx;
    border-radius: 50%;
    background: #d9d9d9;
    &--success {
      background: $l-success;
    }
    &--danger {
      background: $l-danger;
    }
    &--primary {
      background: $l-primary;
    }
    &--warning {
      background: $l-warning;
    }
    &--purple {
      background: #722ed1;
    }
  }
  &__line {
    flex: 1;
    width: 2rpx;
    margin-top: 8rpx;
    background: $l-border;
  }
  &__body {
    flex: 1;
    padding: 0 0 32rpx 12rpx;
  }
  &__head {
    display: flex;
    justify-content: space-between;
  }
  &__node {
    font-size: 28rpx;
    font-weight: 600;
  }
  &__time {
    font-size: 22rpx;
    color: $l-text-3;
  }
  &__user {
    display: flex;
    align-items: center;
    gap: 12rpx;
    margin-top: 8rpx;
    font-size: 26rpx;
    color: $l-text-2;
  }
  &__comment {
    margin-top: 12rpx;
    padding: 16rpx 20rpx;
    border-radius: 12rpx;
    font-size: 26rpx;
    color: $l-text-2;
    background: $l-bg;
    white-space: pre-wrap;
  }
  &__mask {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 100;
    background: rgba(0, 0, 0, 0.45);
    display: flex;
    align-items: flex-end;
  }
  &__sheet {
    width: 100%;
    box-sizing: border-box;
    padding: 32rpx 32rpx calc(32rpx + env(safe-area-inset-bottom));
    background: #fff;
    border-radius: 24rpx 24rpx 0 0;
    &--tall {
      height: 75vh;
      display: flex;
      flex-direction: column;
    }
  }
  &__sheet-title {
    font-size: 32rpx;
    font-weight: 600;
    text-align: center;
    margin-bottom: 16rpx;
  }
  &__sheet-btns {
    display: flex;
    gap: 20rpx;
    margin-top: 32rpx;
    .l-btn {
      flex: 1;
    }
  }
  &__picker {
    display: flex;
    align-items: center;
    justify-content: space-between;
    min-height: 72rpx;
  }
  &__arrow {
    color: #bfbfbf;
    font-size: 32rpx;
  }
  &__user-search {
    margin: 0 0 16rpx;
    background: $l-bg;
  }
  &__users {
    flex: 1;
    height: 0;
  }
  &__user-row {
    display: flex;
    align-items: center;
    gap: 20rpx;
    padding: 20rpx 0;
    border-bottom: 1rpx solid $l-border;
    font-size: 28rpx;
    &--disabled {
      opacity: 0.5;
    }
  }
  &__avatar {
    width: 72rpx;
    height: 72rpx;
    border-radius: 50%;
    background: $l-primary;
    color: #fff;
    text-align: center;
    line-height: 72rpx;
    font-size: 30rpx;
  }
  &__user-info {
    flex: 1;
  }
  &__user-sub {
    margin-top: 4rpx;
    font-size: 22rpx;
  }
}
</style>
