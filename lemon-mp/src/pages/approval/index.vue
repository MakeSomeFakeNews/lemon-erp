<template>
  <view>
    <view class="ap__top">
      <l-segmented v-model="tab" :options="tabs" @change="paged.refresh()" />
      <view class="l-search">
        <input v-model="keyword" :placeholder="t('approval.searchPh')" placeholder-class="l-placeholder" confirm-type="search" @confirm="paged.refresh()" />
      </view>
    </view>

    <template v-if="tab === 'mine'">
      <view v-for="r in paged.list" :key="r.id" class="l-item" @click="open(r.id)">
        <view class="l-item__head">
          <text class="l-item__title">{{ r.name }}</text>
          <l-tag :text="dictText(dict.wf_status, r.status)" :color="dictColor(dict.wf_status, r.status)" />
        </view>
        <view class="l-item__desc"><text class="ap__label">{{ t('approval.process') }}</text>{{ r.processName }}</view>
        <view v-if="r.status === FlowStatus.RUNNING && r.currentNodes" class="l-item__desc"><text class="ap__label">{{ t('approval.currentNode') }}</text>{{ r.currentNodes }}</view>
        <view class="l-item__desc"><text class="ap__label">{{ t('approval.startTime') }}</text>{{ shortTime(r.startTime) }}</view>
      </view>
    </template>
    <template v-else-if="tab === 'copy'">
      <view v-for="r in paged.list" :key="r.id" class="l-item" @click="open(r.processInstanceId)">
        <view class="l-item__head">
          <text class="l-item__title">{{ r.processName }}</text>
          <l-tag v-if="r.status" :text="dictText(dict.wf_status, r.status)" :color="dictColor(dict.wf_status, r.status)" />
        </view>
        <view class="l-item__desc"><text class="ap__label">{{ t('approval.starter') }}</text>{{ r.startUserName }}</view>
        <view v-if="r.nodeName" class="l-item__desc"><text class="ap__label">{{ t('approval.copy.node') }}</text>{{ r.nodeName }}</view>
        <view class="l-item__desc"><text class="ap__label">{{ t('approval.copy.time') }}</text>{{ shortTime(r.createTime) }}</view>
      </view>
    </template>
    <template v-else>
      <view v-for="r in paged.list" :key="r.taskId" class="l-item" @click="open(r.processInstanceId)">
        <view class="l-item__head">
          <text class="l-item__title">{{ r.processName }}</text>
          <l-tag v-if="tab === 'done' && r.action" :text="dictText(dict.wf_task_action, r.action)" :color="dictColor(dict.wf_task_action, r.action)" />
          <l-tag v-else-if="tab === 'todo'" :text="r.taskName" color="warning" />
        </view>
        <view class="l-item__desc"><text class="ap__label">{{ t('approval.starter') }}</text>{{ r.startUserName }}</view>
        <view v-if="tab === 'todo'" class="l-item__desc"><text class="ap__label">{{ t('approval.arriveTime') }}</text>{{ shortTime(r.createTime) }}</view>
        <view v-else class="l-item__desc">
          <text class="ap__label">{{ t('approval.handleTime') }}</text>{{ shortTime(r.endTime) }}
          <text v-if="r.status" class="ap__status">{{ dictText(dict.wf_status, r.status) }}</text>
        </view>
      </view>
    </template>

    <l-empty v-if="!tabs.length || (paged.loaded && !paged.list.length)" />
    <l-load-more v-else-if="paged.list.length" :status="paged.status" />
  </view>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { takeTabLink } from '@/config/entries'
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { FlowStatus } from '@web/constants/enums'
import { pageCopy, pageDone, pageMine, pageTodo } from '@/api/workflow'
import { usePaged } from '@/composables/usePaged'
import { useTitle } from '@/composables/useTitle'
import { dictColor, dictText, useDict } from '@/store/dict'
import { hasPerm, userState } from '@/store/user'
import { ensureLogin } from '@/utils/session'
import { shortTime } from '@/utils/format'

const { t } = useI18n()
useTitle('approval.title')
const dict = useDict('wf_status', 'wf_task_action')

const TABS = [
  { value: 'todo', perm: 'wf:task:todo', fetch: pageTodo },
  { value: 'done', perm: 'wf:task:done', fetch: pageDone },
  { value: 'mine', perm: 'wf:instance:mine', fetch: pageMine },
  { value: 'copy', perm: 'wf:copy:list', fetch: pageCopy }
]
const LINK_TABS = [
  ['/workflow/done', 'done'],
  ['/workflow/mine', 'mine'],
  ['/workflow/copy', 'copy']
]
const tab = ref('todo')
const keyword = ref('')
const todoCount = ref(0)

const tabs = computed(() =>
  userState.loaded
    ? TABS.filter((x) => hasPerm(x.perm)).map((x) => ({
        label: t('approval.tab.' + x.value),
        value: x.value,
        badge: x.value === 'todo' ? todoCount.value : 0
      }))
    : []
)

// 没有当前分段的权限时切到第一个有权限的分段
watch(tabs, (list) => {
  if (list.length && !list.some((x) => x.value === tab.value)) tab.value = list[0].value
})

const paged = usePaged((p) => {
  const current = TABS.find((x) => x.value === tab.value)
  if (!hasPerm(current.perm)) return Promise.resolve({ records: [], total: 0 })
  return current.fetch({ ...p, name: keyword.value.trim() || undefined }).then((res) => {
    if (tab.value === 'todo' && p.current === 1) todoCount.value = res?.total || 0
    return res
  })
})

function refreshTodoCount() {
  if (tab.value === 'todo' || !hasPerm('wf:task:todo')) return
  pageTodo({ current: 1, size: 1 })
    .then((res) => (todoCount.value = res?.total || 0))
    .catch(() => {})
}

// 从详情返回时刷新（审批后待办减少）
onShow(async () => {
  if (!(await ensureLogin())) return
  // 从待办、消息的链接进入时切到对应分段（/workflow/done、/workflow/mine、/workflow/copy，其余为待办）；发起申请后进入「我发起的」
  const link = takeTabLink('/pages/approval/index')
  if (link) tab.value = LINK_TABS.find(([prefix]) => link.startsWith(prefix))?.[1] || 'todo'
  if (tabs.value.length && !tabs.value.some((x) => x.value === tab.value)) tab.value = tabs.value[0].value
  paged.refresh()
  refreshTodoCount()
})
onPullDownRefresh(() => {
  paged.refresh()
  refreshTodoCount()
})
onReachBottom(() => paged.more())

function open(id) {
  uni.navigateTo({ url: `/pkg-approval/detail?id=${encodeURIComponent(id)}` })
}
</script>

<style lang="scss">
.ap {
  &__top {
    position: sticky;
    top: 0;
    z-index: 5;
    background: $l-bg;
    padding-bottom: 4rpx;
  }
  &__label {
    margin-right: 12rpx;
    color: #bfbfbf;
  }
  &__status {
    margin-left: 16rpx;
  }
}
</style>
