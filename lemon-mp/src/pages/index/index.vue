<template>
  <view class="wb">
    <view class="wb__head">
      <view class="wb__hello">{{ t('common.hello', { name: userState.user?.nickname || userState.user?.username || '' }) }}</view>
      <view class="wb__sub">{{ [userState.tenantName, userState.user?.deptName].filter(Boolean).join(' · ') }}</view>
    </view>

    <!-- 经营概览：有对应权限时才有数据 -->
    <view v-if="cards.length" class="wb__cards">
      <view v-for="c in cards" :key="c.key" class="wb__card">
        <view class="wb__card-label">{{ c.label }}</view>
        <view class="wb__card-value">{{ c.kind === 'MONEY' ? shortMoney(c.value) : qty(c.value) }}</view>
        <view v-if="c.previous !== null && c.previous !== undefined && Number(c.previous) !== 0" class="wb__card-rate" :class="rate(c) >= 0 ? 'up' : 'down'">
          {{ t('common.vsLastMonth', { rate: (rate(c) >= 0 ? '+' : '') + rate(c) + '%' }) }}
        </view>
      </view>
    </view>

    <l-card :title="t('common.todo')">
      <view v-if="!todos.length" class="l-muted wb__none">{{ t('common.noTodo') }}</view>
      <view v-for="(item, i) in todos" :key="i" class="wb__todo" @click="openLink(item.link)">
        <view class="wb__dot" :class="'wb__dot--' + item.level" />
        <text class="wb__todo-title">{{ item.title }}</text>
        <text class="wb__todo-count">{{ item.count }}</text>
        <text class="wb__arrow">›</text>
      </view>
    </l-card>

    <l-card v-for="g in groups" :key="g.key" :title="t('common.group.' + g.key)">
      <view class="wb__grid">
        <view v-for="e in g.entries" :key="e.key" class="wb__entry" @click="openEntry(e)">
          <view class="wb__icon" :style="{ background: e.color }">{{ t(e.title).slice(0, 1) }}</view>
          <text class="wb__entry-title">{{ t(e.title) }}</text>
        </view>
      </view>
    </l-card>
    <l-empty v-if="userState.loaded && !groups.length" :text="t('common.noEntries')" />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { userState } from '@/store/user'
import { ensureLogin } from '@/utils/session'
import { applyTabBar, setMessageBadge } from '@/utils/tabbar'
import { getBusiness, getTodos } from '@/api/dashboard'
import { unreadCount } from '@/api/message'
import { GROUPS, entryOfLink, openEntry, visibleEntries } from '@/config/entries'
import { qty, shortMoney } from '@/utils/format'
import { useTitle } from '@/composables/useTitle'
import { requestSubscribe } from '@/utils/subscribe'

const { t } = useI18n()
useTitle('common.tab.workbench')
const todos = ref([])
const cards = ref([])

const groups = computed(() => {
  if (!userState.loaded) return []
  const entries = visibleEntries()
  return GROUPS.map((key) => ({ key, entries: entries.filter((e) => e.group === key) })).filter((g) => g.entries.length)
})

const rate = (c) => Math.round(((Number(c.value) - Number(c.previous)) / Math.abs(Number(c.previous))) * 1000) / 10

async function load() {
  const [td, bz, unread] = await Promise.all([getTodos().catch(() => []), getBusiness().catch(() => []), unreadCount().catch(() => 0)])
  todos.value = td || []
  cards.value = bz || []
  setMessageBadge(unread)
}

onShow(async () => {
  applyTabBar()
  if (await ensureLogin()) load()
})

onPullDownRefresh(async () => {
  await load()
  uni.stopPullDownRefresh()
})

function openLink(link) {
  requestSubscribe()
  const entry = entryOfLink(link)
  if (entry) openEntry(entry, link)
  else uni.showToast({ title: t('common.onlyOnPc'), icon: 'none' })
}
</script>

<style lang="scss">
.wb {
  padding-bottom: 24rpx;
  &__head {
    padding: 40rpx 32rpx 8rpx;
  }
  &__hello {
    font-size: 40rpx;
    font-weight: 600;
  }
  &__sub {
    margin-top: 8rpx;
    font-size: 24rpx;
    color: $l-text-3;
  }
  &__cards {
    display: flex;
    flex-wrap: wrap;
    gap: 20rpx;
    padding: 24rpx 24rpx 0;
  }
  &__card {
    width: calc(50% - 10rpx);
    box-sizing: border-box;
    padding: 24rpx;
    background: #fff;
    border-radius: $l-radius;
  }
  &__card-label {
    font-size: 24rpx;
    color: $l-text-3;
  }
  &__card-value {
    margin-top: 8rpx;
    font-size: 36rpx;
    font-weight: 600;
  }
  &__card-rate {
    margin-top: 4rpx;
    font-size: 22rpx;
    &.up {
      color: $l-success;
    }
    &.down {
      color: $l-danger;
    }
  }
  &__none {
    padding: 16rpx 0;
    font-size: 26rpx;
  }
  &__todo {
    display: flex;
    align-items: center;
    padding: 20rpx 0;
    border-bottom: 1rpx solid $l-border;
    &:last-child {
      border-bottom: none;
    }
  }
  &__dot {
    width: 14rpx;
    height: 14rpx;
    border-radius: 50%;
    margin-right: 16rpx;
    background: $l-primary;
    &--danger {
      background: $l-danger;
    }
    &--warning {
      background: $l-warning;
    }
  }
  &__todo-title {
    flex: 1;
    font-size: 28rpx;
  }
  &__todo-count {
    font-size: 30rpx;
    font-weight: 600;
    color: $l-text;
  }
  &__arrow {
    margin-left: 12rpx;
    color: #bfbfbf;
  }
  &__grid {
    display: flex;
    flex-wrap: wrap;
  }
  &__entry {
    width: 25%;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 16rpx 0;
  }
  &__icon {
    width: 88rpx;
    height: 88rpx;
    border-radius: 24rpx;
    color: #fff;
    font-size: 36rpx;
    font-weight: 600;
    line-height: 88rpx;
    text-align: center;
  }
  &__entry-title {
    margin-top: 12rpx;
    font-size: 24rpx;
    color: $l-text-2;
    text-align: center;
  }
}
</style>
