<template>
  <view class="mine">
    <!-- 个人卡片 -->
    <view class="mine__hero">
      <view v-if="userState.tenantName" class="mine__tenant">{{ userState.tenantName }}</view>
      <view class="mine__profile">
        <image v-if="avatar" class="mine__avatar" :src="avatar" mode="aspectFill" />
        <view v-else class="mine__avatar mine__avatar--text">{{ displayName.slice(0, 1) }}</view>
        <view class="mine__who">
          <view class="mine__name">{{ displayName }}</view>
          <view class="mine__meta">
            <text>{{ userState.user?.username }}</text>
            <text v-if="profile.deptName" class="mine__dot">·</text>
            <text v-if="profile.deptName">{{ profile.deptName }}</text>
          </view>
          <view v-if="profile.roleNames.length" class="mine__roles">
            <text v-for="r in profile.roleNames.slice(0, 3)" :key="r" class="mine__role">{{ r }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 个人数据：点击进入对应页面 -->
    <view v-if="stats.length" class="mine__stats">
      <view v-for="s in stats" :key="s.key" class="mine__stat" @click="s.open">
        <text class="mine__stat-value" :class="{ 'mine__stat-value--alert': s.alert }">{{ s.value }}</text>
        <text class="mine__stat-label">{{ s.label }}</text>
      </view>
    </view>

    <!-- 常用 -->
    <view v-if="shortcuts.length" class="mine__panel">
      <view class="mine__panel-title">{{ t('common.mine.shortcuts') }}</view>
      <view class="mine__grid">
        <view v-for="s in shortcuts" :key="s.key" class="mine__shortcut" @click="s.open">
          <view class="mine__shortcut-icon" :style="{ background: s.color }">
            <image class="mine__shortcut-img" :src="'/static/icons/' + s.icon + '.png'" />
          </view>
          <text class="mine__shortcut-label">{{ s.label }}</text>
        </view>
      </view>
    </view>

    <!-- 账号资料 -->
    <view class="mine__panel">
      <view class="mine__panel-title">{{ t('common.mine.account') }}</view>
      <l-cell icon="building" icon-color="#2f54eb" :label="t('common.mine.dept')" :value="profile.deptName" />
      <l-cell icon="shield" icon-color="#722ed1" :label="t('common.mine.roles')" :value="profile.roleNames.join('、')" />
      <l-cell icon="phone" icon-color="#13c2c2" :label="t('common.mine.phone')" :value="userState.user?.phone" />
      <l-cell icon="mail" icon-color="#fa8c16" :label="t('common.mine.email')" :value="userState.user?.email" />
    </view>

    <!-- 设置 -->
    <view class="mine__panel">
      <view class="mine__panel-title">{{ t('common.mine.settings') }}</view>
      <picker :range="LOCALES" range-key="label" :value="localeIndex" @change="changeLocale">
        <l-cell icon="language" icon-color="#1677ff" :label="t('common.mine.language')" :value="LOCALES[localeIndex].label" arrow />
      </picker>
      <!-- #ifdef MP-WEIXIN -->
      <l-cell
        v-if="subscribeAvailable()"
        icon="bell"
        icon-color="#faad14"
        :label="t('common.subscribe.title')"
        :value="t('common.subscribe.tip')"
        arrow
        @click="requestSubscribe(true)"
      />
      <!-- #endif -->
      <l-cell icon="lock" icon-color="#52c41a" :label="t('common.mine.password')" arrow @click="go('/pages/mine/password')" />
      <!-- #ifdef MP-WEIXIN -->
      <l-cell icon="swap" icon-color="#13c2c2" :label="t('common.mine.switchAccount')" arrow @click="switchAccount" />
      <l-cell icon="unlink" icon-color="#8c8c8c" :label="t('common.mine.unbind')" arrow @click="unbind" />
      <!-- #endif -->
    </view>

    <view class="mine__logout" @click="doLogout">{{ t('common.mine.logout') }}</view>

    <view class="mine__footer">
      <image class="mine__logo" src="/static/logo.png" mode="aspectFit" />
      <text>{{ t('common.appName') }} {{ VERSION }}</text>
    </view>
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { FlowStatus } from '@web/constants/enums'
import { LOCALES, locale, setLocale, useI18n } from '@/locales'
import { hasPerm, resetUser, userState } from '@/store/user'
import { getProfile, logout, miniappUnbind } from '@/api/auth'
import { pageMine, pageTodo } from '@/api/workflow'
import { unreadCount } from '@/api/message'
import { selfProfile } from '@/api/hr'
import { runAction } from '@/composables/useAction'
import { useTitle } from '@/composables/useTitle'
import { ensureLogin } from '@/utils/session'
import { applyTabBar, setMessageBadge } from '@/utils/tabbar'
import { fileUrl } from '@/utils/request'
import { requestSubscribe, subscribeAvailable } from '@/utils/subscribe'
import { openEntry, visibleEntries } from '@/config/entries'

const VERSION = '1.0.0'
const { t } = useI18n()
useTitle('common.tab.mine')

const profile = reactive({ deptName: '', roleNames: [] })
/** 个人数据：待我审批、审批中的申请、未读消息、年假余额（没有权限或没有员工档案的不显示） */
const counts = reactive({ todo: null, running: null, unread: null, annual: null })

const displayName = computed(() => userState.user?.nickname || userState.user?.username || '')
const avatar = computed(() => fileUrl(userState.user?.avatar))
const localeIndex = computed(() => Math.max(0, LOCALES.findIndex((l) => l.value === locale.value)))

const entry = (key) => visibleEntries().find((e) => e.key === key)
const openKey = (key, link) => () => {
  const e = entry(key)
  if (e) openEntry(e, link)
}

const stats = computed(() =>
  [
    counts.todo !== null && { key: 'todo', value: counts.todo, label: t('common.mine.statTodo'), alert: counts.todo > 0, open: openKey('approval', '/workflow/todo') },
    counts.running !== null && { key: 'running', value: counts.running, label: t('common.mine.statRunning'), open: openKey('approval', '/workflow/mine') },
    counts.unread !== null && { key: 'unread', value: counts.unread, label: t('common.mine.statUnread'), alert: counts.unread > 0, open: () => uni.switchTab({ url: '/pages/message/index' }) },
    counts.annual !== null && { key: 'annual', value: counts.annual, label: t('common.mine.statAnnual'), open: openKey('hr-attendance') }
  ].filter(Boolean)
)

/** 常用：按入口权限显示 */
const SHORTCUTS = [
  { key: 'approval', icon: 'document', color: '#1677ff', label: 'common.mine.myApplications', link: '/workflow/mine' },
  { key: 'hr-clock', icon: 'clock', color: '#13c2c2', label: 'hr.entry.clock' },
  { key: 'hr-leave', icon: 'calendar', color: '#fa8c16', label: 'hr.entry.leave' },
  { key: 'hr-payslip', icon: 'wallet', color: '#52c41a', label: 'hr.entry.payslip' }
]
const shortcuts = computed(() =>
  userState.loaded
    ? SHORTCUTS.filter((s) => entry(s.key) && (s.key !== 'approval' || hasPerm('wf:instance:mine'))).map((s) => ({
        ...s,
        label: t(s.label),
        open: openKey(s.key, s.link)
      }))
    : []
)

async function load() {
  const [p, todo, running, unread, hr] = await Promise.all([
    getProfile().catch(() => null),
    hasPerm('wf:task:todo') ? pageTodo({ current: 1, size: 1 }).catch(() => null) : null,
    hasPerm('wf:instance:mine') ? pageMine({ current: 1, size: 1, status: FlowStatus.RUNNING }).catch(() => null) : null,
    unreadCount().catch(() => null),
    // 没有关联员工档案时报错，不显示年假
    selfProfile().catch(() => null)
  ])
  profile.deptName = p?.deptName || ''
  profile.roleNames = p?.roleNames || []
  counts.todo = todo ? todo.total : null
  counts.running = running ? running.total : null
  counts.unread = unread ?? null
  counts.annual = hr && hr.annualDays !== undefined ? Math.max(0, +(hr.annualDays - Number(hr.annualUsedDays || 0)).toFixed(1)) : null
  if (unread !== null) setMessageBadge(unread)
}

onShow(async () => {
  if (await ensureLogin()) load()
})

onPullDownRefresh(async () => {
  await load()
  uni.stopPullDownRefresh()
})

function go(url) {
  uni.navigateTo({ url })
}

function changeLocale(e) {
  setLocale(LOCALES[e.detail.value].value)
  applyTabBar()
  uni.setNavigationBarTitle({ title: t('common.tab.mine') })
}

async function signOut(url) {
  await logout().catch(() => {})
  resetUser()
  uni.reLaunch({ url })
}

function doLogout() {
  // 退出后回到登录页但不自动登录，否则绑定了微信的账号会立即重新登录
  runAction(() => Promise.resolve(), { confirm: t('common.mine.logoutConfirm'), success: ' ', then: () => signOut('/pages/login/index?switch=1') })
}

function switchAccount() {
  signOut('/pages/login/index?switch=1')
}

function unbind() {
  runAction(() => miniappUnbind(), { confirm: t('common.mine.unbindConfirm'), success: t('common.mine.unbound'), then: () => signOut('/pages/login/index?switch=1') })
}
</script>

<style lang="scss">
.mine {
  padding-bottom: 32rpx;
  &__hero {
    position: relative;
    padding: 56rpx 40rpx 120rpx;
    background: linear-gradient(135deg, #1677ff 0%, #4096ff 60%, #69b1ff 100%);
    color: #fff;
    overflow: hidden;
    &::after {
      content: '';
      position: absolute;
      right: -80rpx;
      top: -120rpx;
      width: 360rpx;
      height: 360rpx;
      border-radius: 50%;
      background: rgba(255, 255, 255, 0.08);
    }
  }
  &__tenant {
    position: absolute;
    top: 24rpx;
    right: 32rpx;
    z-index: 1;
    padding: 4rpx 18rpx;
    border-radius: 20rpx;
    font-size: 22rpx;
    background: rgba(255, 255, 255, 0.2);
  }
  &__profile {
    position: relative;
    z-index: 1;
    display: flex;
    align-items: center;
    gap: 28rpx;
  }
  &__avatar {
    flex-shrink: 0;
    width: 128rpx;
    height: 128rpx;
    border-radius: 50%;
    border: 4rpx solid rgba(255, 255, 255, 0.85);
    background: #fff;
    &--text {
      color: $l-primary;
      font-size: 56rpx;
      font-weight: 600;
      line-height: 120rpx;
      text-align: center;
    }
  }
  &__who {
    flex: 1;
    min-width: 0;
  }
  &__name {
    font-size: 40rpx;
    font-weight: 600;
  }
  &__meta {
    margin-top: 8rpx;
    font-size: 24rpx;
    opacity: 0.85;
  }
  &__dot {
    margin: 0 10rpx;
  }
  &__roles {
    display: flex;
    flex-wrap: wrap;
    gap: 12rpx;
    margin-top: 16rpx;
  }
  &__role {
    padding: 2rpx 16rpx;
    border-radius: 8rpx;
    font-size: 22rpx;
    background: rgba(255, 255, 255, 0.2);
  }
  &__stats {
    position: relative;
    z-index: 2;
    display: flex;
    margin: -80rpx 24rpx 0;
    padding: 28rpx 0;
    background: #fff;
    border-radius: $l-radius;
    box-shadow: 0 8rpx 24rpx rgba(22, 119, 255, 0.08);
  }
  &__stat {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    position: relative;
    & + &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 12rpx;
      bottom: 12rpx;
      width: 1rpx;
      background: $l-border;
    }
  }
  &__stat-value {
    font-size: 40rpx;
    font-weight: 600;
    color: $l-text;
    &--alert {
      color: $l-primary;
    }
  }
  &__stat-label {
    margin-top: 6rpx;
    font-size: 22rpx;
    color: $l-text-3;
  }
  &__panel {
    margin: 24rpx 24rpx 0;
    padding: 8rpx 28rpx;
    background: #fff;
    border-radius: $l-radius;
  }
  &__panel-title {
    padding: 20rpx 0 8rpx;
    font-size: 26rpx;
    font-weight: 600;
    color: $l-text-2;
  }
  &__grid {
    display: flex;
    padding: 8rpx 0 20rpx;
  }
  &__shortcut {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
  }
  &__shortcut-icon {
    width: 88rpx;
    height: 88rpx;
    border-radius: 24rpx;
    display: flex;
    align-items: center;
    justify-content: center;
  }
  &__shortcut-img {
    width: 48rpx;
    height: 48rpx;
  }
  &__shortcut-label {
    margin-top: 12rpx;
    font-size: 24rpx;
    color: $l-text-2;
  }
  &__logout {
    margin: 24rpx 24rpx 0;
    padding: 28rpx 0;
    text-align: center;
    font-size: 30rpx;
    color: $l-danger;
    background: #fff;
    border-radius: $l-radius;
  }
  &__footer {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12rpx;
    padding: 40rpx 0 16rpx;
    font-size: 22rpx;
    color: $l-text-3;
  }
  &__logo {
    width: 32rpx;
    height: 32rpx;
  }
}
</style>
