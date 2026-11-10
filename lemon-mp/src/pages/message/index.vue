<template>
  <view>
    <view class="msg__bar">
      <l-segmented v-model="tab" :options="tabs" class="msg__seg" @change="paged.refresh()" />
      <text class="msg__all" @click="readAll">{{ t('common.message.readAll') }}</text>
    </view>
    <!-- 通知公告：有未读时提示，点击进入公告列表 -->
    <view v-if="noticeUnread" class="msg__notice" @click="openNotices">
      <text class="msg__notice-title">{{ t('notice.title') }}</text>
      <text class="msg__notice-count">{{ t('notice.unreadN', { n: noticeUnread }) }}</text>
      <text class="msg__notice-arrow">›</text>
    </view>
    <view v-for="m in paged.list" :key="m.id" class="l-item" @click="open(m)">
      <view class="l-item__head">
        <view v-if="!m.readFlag" class="msg__dot" />
        <text class="l-item__title">{{ m.title }}</text>
        <l-tag v-if="m.category" :text="dictText(dict.sys_message_category, m.category)" :color="dictColor(dict.sys_message_category, m.category)" />
      </view>
      <view v-if="m.content" class="l-item__desc msg__content">{{ m.content }}</view>
      <view class="l-item__desc">{{ shortTime(m.createTime) }}</view>
    </view>
    <l-empty v-if="paged.loaded && !paged.list.length" />
    <l-load-more v-else-if="paged.list.length" :status="paged.status" />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { pageMessages, readMessages, unreadCount } from '@/api/message'
import { noticeUnreadCount } from '@/api/notice'
import { usePaged } from '@/composables/usePaged'
import { runAction } from '@/composables/useAction'
import { useTitle } from '@/composables/useTitle'
import { dictColor, dictText, useDict } from '@/store/dict'
import { ensureLogin } from '@/utils/session'
import { setMessageBadge } from '@/utils/tabbar'
import { shortTime } from '@/utils/format'
import { entryOfLink, openEntry } from '@/config/entries'

const { t } = useI18n()
useTitle('common.tab.message')
const dict = useDict('sys_message_category')
const tab = ref('unread')
const tabs = computed(() => [
  { label: t('common.message.unread'), value: 'unread' },
  { label: t('common.message.all'), value: 'all' }
])
const paged = usePaged((p) => pageMessages({ ...p, unread: tab.value === 'unread' ? true : undefined }))

const noticeUnread = ref(0)
const refreshBadge = () => {
  unreadCount().then(setMessageBadge).catch(() => {})
  noticeUnreadCount().then((n) => (noticeUnread.value = n)).catch(() => {})
}
const openNotices = () => uni.navigateTo({ url: '/pkg-notice/index' })

onShow(async () => {
  if (await ensureLogin()) {
    paged.refresh()
    refreshBadge()
  }
})
onPullDownRefresh(() => {
  paged.refresh()
  refreshBadge()
})
onReachBottom(() => paged.more())

async function open(m) {
  if (!m.readFlag) {
    await readMessages([m.id]).catch(() => {})
    m.readFlag = 1
    refreshBadge()
  }
  const entry = entryOfLink(m.link)
  if (entry) openEntry(entry, m.link)
  else if (m.link) uni.showToast({ title: t('common.onlyOnPc'), icon: 'none' })
}

function readAll() {
  runAction(() => readMessages(), {
    confirm: t('common.message.readAllConfirm'),
    then: () => {
      paged.refresh()
      refreshBadge()
    }
  })
}
</script>

<style lang="scss">
.msg {
  &__bar {
    display: flex;
    align-items: center;
    background: #fff;
    position: sticky;
    top: 0;
    z-index: 5;
  }
  &__seg {
    flex: 1;
  }
  &__all {
    padding: 0 28rpx;
    font-size: 26rpx;
    color: $l-primary;
  }
  &__dot {
    width: 14rpx;
    height: 14rpx;
    border-radius: 50%;
    background: $l-danger;
  }
  &__notice {
    display: flex;
    align-items: center;
    gap: 16rpx;
    margin: 16rpx 24rpx 0;
    padding: 24rpx 28rpx;
    border-radius: 12rpx;
    background: #fff7e6;
  }
  &__notice-title {
    flex: 1;
    font-size: 28rpx;
    font-weight: 600;
    color: #d46b08;
  }
  &__notice-count {
    font-size: 26rpx;
    color: #d46b08;
  }
  &__notice-arrow {
    font-size: 34rpx;
    color: #d46b08;
  }
  &__content {
    color: $l-text-2;
    font-size: 26rpx;
  }
}
</style>
