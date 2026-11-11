<template>
  <header class="header-bar">
    <!-- 顶部菜单布局：Logo 在顶栏 -->
    <router-link v-if="layout === LayoutMode.TOP && !mobile" to="/" class="header-logo">
      <AppLogo :size="28" />
      <span class="header-logo-title">{{ configStore.systemName }}</span>
    </router-link>
    <a-button v-else-if="showToggle" type="text" class="header-btn" @click="emit('toggle')">
      <template #icon>
        <MenuUnfoldOutlined v-if="collapsed" />
        <MenuFoldOutlined v-else />
      </template>
    </a-button>

    <!-- 桌面端：顶部模块导航 / 顶部菜单 / 面包屑（侧边菜单、双栏布局）；移动端：当前页面 -->
    <a-menu
      v-if="!mobile && layout === LayoutMode.MIX"
      mode="horizontal"
      class="module-nav"
      :items="moduleItems"
      :selected-keys="activeModule ? [activeModule.key] : [route.path]"
      @click="({ key }) => openModule(key)"
    />
    <TopMenu v-else-if="!mobile && layout === LayoutMode.TOP" />
    <a-breadcrumb v-else class="breadcrumb">
      <!-- 桌面端显示完整层级；窄屏空间有限，只显示当前页面（所属模块可从菜单抽屉看到） -->
      <a-breadcrumb-item v-for="(item, i) in mobile ? breadcrumbs.slice(-1) : breadcrumbs" :key="i">
        <MenuIcon v-if="item.icon && (mobile || i === 0)" :name="item.icon" />
        <span>{{ item.title }}</span>
      </a-breadcrumb-item>
    </a-breadcrumb>

    <div class="header-right">
      <a-tooltip v-if="configStore.demoMode" :title="$t('layout.demoTip')">
        <a-tag color="orange" :bordered="false" class="demo-tag">{{ $t('layout.demoMode') }}</a-tag>
      </a-tooltip>
      <MenuSearch :compact="layout === LayoutMode.TOP" />
      <MessageBell />
      <LangSwitch btn-class="header-btn" />
      <a-tooltip :title="appStore.dark ? $t('layout.toLight') : $t('layout.toDark')">
        <a-button type="text" class="header-btn" @click="appStore.toggleDark()">
          <template #icon>
            <ThemeIcon :dark="appStore.dark" />
          </template>
        </a-button>
      </a-tooltip>

      <a-tooltip :title="$t('layout.settings.title')">
        <a-button type="text" class="header-btn" @click="emit('settings')">
          <template #icon><SkinOutlined /></template>
        </a-button>
      </a-tooltip>

      <a-dropdown placement="bottomRight">
        <div class="user-entry">
          <UserAvatar :src="userStore.user?.avatar" :name="userStore.displayName" size="small" />
          <span class="user-text hide-mobile">
            <span class="user-name">{{ userStore.displayName }}</span>
            <!-- SaaS 模式下显示当前租户 -->
            <span v-if="userStore.tenantName" class="tenant-name">{{ userStore.tenantName }}</span>
          </span>
          <DownOutlined class="hide-mobile" style="font-size: 10px" />
        </div>
        <template #overlay>
          <a-menu>
            <template v-if="userStore.tenantName">
              <a-menu-item key="tenant" disabled class="tenant-item">
                <template #icon><ShopOutlined /></template>
                {{ userStore.tenantName }}
              </a-menu-item>
              <a-menu-divider />
            </template>
            <a-menu-item key="profile" @click="router.push('/profile')">
              <template #icon><UserOutlined /></template>
              {{ $t('route.profile') }}
            </a-menu-item>
            <a-menu-item key="messages" @click="router.push('/messages')">
              <template #icon><BellOutlined /></template>
              {{ $t('route.messages') }}
            </a-menu-item>
            <a-menu-divider />
            <a-menu-item key="logout" danger @click="confirmLogout">
              <template #icon><LogoutOutlined /></template>
              {{ $t('common.action.logout') }}
            </a-menu-item>
          </a-menu>
        </template>
      </a-dropdown>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Modal } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import {
  BellOutlined,
  DownOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  ShopOutlined,
  SkinOutlined,
  UserOutlined
} from '@ant-design/icons-vue'
import AppLogo from '@/components/AppLogo.vue'
import MenuIcon from '@/components/MenuIcon.vue'
import MenuSearch from './MenuSearch.vue'
import MessageBell from './MessageBell.vue'
import TopMenu from './TopMenu.vue'
import LangSwitch from '@/components/LangSwitch.vue'
import ThemeIcon from '@/components/ThemeIcon.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import { useAppStore } from '@/store/app'
import { useConfigStore } from '@/store/config'
import { useUserStore } from '@/store/user'
import { buildBreadcrumbIndex } from '@/utils/menu'
import { LayoutMode } from '@/utils/uiSettings'
import { useModuleNav } from '../useModuleNav'
import { homeItem, menuIcon, shortModuleTitle } from '../menuItems'

defineProps({
  collapsed: Boolean,
  mobile: Boolean,
  /** 布局（utils/uiSettings 的 LayoutMode），移动端不区分 */
  layout: { type: String, default: LayoutMode.MIX },
  /** 显示折叠侧边栏按钮（没有侧边栏时不显示） */
  showToggle: { type: Boolean, default: true }
})
const emit = defineEmits(['toggle', 'settings'])

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const configStore = useConfigStore()
const userStore = useUserStore()

const { modules, activeModule, openModule } = useModuleNav()
const moduleItems = computed(() => [
  homeItem(),
  ...modules.value.map((m) => ({ key: m.key, label: shortModuleTitle(m.title), icon: menuIcon(m.icon) }))
])

const breadcrumbIndex = computed(() => buildBreadcrumbIndex(userStore.menus))
const breadcrumbs = computed(() => {
  const chain = breadcrumbIndex.value.get(route.path)
  if (chain) return chain
  return route.meta?.title ? [{ title: route.meta.title, icon: route.meta.icon }] : []
})

function confirmLogout() {
  Modal.confirm({
    title: t('common.action.logout'),
    content: t('layout.logoutConfirm'),
    okText: t('layout.logoutOk'),
    async onOk() {
      await userStore.logout()
      // 刷新页面以清空已注册的动态路由
      location.href = '/login'
    }
  })
}
</script>

<style scoped>
.header-bar {
  height: 56px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 12px 0 8px;
  background: var(--app-header);
  border-bottom: 1px solid var(--app-border);
}
.header-btn {
  font-size: 16px;
}
.header-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 12px 0 8px;
  flex: none;
}
.header-logo-title {
  color: var(--app-text);
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0.5px;
  white-space: nowrap;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
}
.breadcrumb {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
}
.module-nav {
  flex: 1;
  min-width: 0;
  height: 55px;
  line-height: 55px;
  border-bottom: none;
  background: transparent;
}
.module-nav :deep(.ant-menu-item),
.module-nav :deep(.ant-menu-submenu) {
  padding-inline: 14px;
}
.module-nav :deep(.ant-menu-item .anticon) {
  font-size: 15px;
}
.breadcrumb :deep(ol) {
  flex-wrap: nowrap;
}
.breadcrumb :deep(li) {
  overflow: hidden;
  text-overflow: ellipsis;
}
.breadcrumb :deep(.anticon) {
  margin-right: 4px;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 2px;
}
.demo-tag {
  margin-right: 6px;
  cursor: default;
}
.user-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 40px;
  padding: 0 8px;
  margin-left: 4px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}
.user-entry:hover {
  background: var(--app-hover);
}
.user-text {
  display: flex;
  flex-direction: column;
  justify-content: center;
  line-height: 1.25;
}
.user-name,
.tenant-name {
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.tenant-name {
  font-size: 12px;
  color: var(--app-text-3);
}

@media (max-width: 575px) {
  .hide-mobile {
    display: none !important;
  }
}
</style>

<style>
/* 下拉菜单渲染在 body 下，禁用项的灰色需要非 scoped 样式覆盖：租户名只是展示，不是不可用的功能 */
.ant-dropdown-menu .ant-dropdown-menu-item.tenant-item,
.ant-dropdown-menu .ant-dropdown-menu-item.tenant-item .ant-dropdown-menu-item-icon {
  color: var(--app-text-2);
  cursor: default;
}
</style>
