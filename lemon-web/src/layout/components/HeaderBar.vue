<template>
  <header class="header-bar">
    <a-button type="text" class="header-btn" @click="emit('toggle')">
      <template #icon>
        <MenuUnfoldOutlined v-if="collapsed" />
        <MenuFoldOutlined v-else />
      </template>
    </a-button>

    <!-- 桌面端：顶部模块导航；移动端：面包屑 -->
    <a-menu
      v-if="!mobile"
      mode="horizontal"
      class="module-nav"
      :items="moduleItems"
      :selected-keys="activeModule ? [activeModule.key] : [route.path]"
      @click="({ key }) => openModule(key)"
    />
    <a-breadcrumb v-else class="breadcrumb">
      <!-- 窄屏空间有限，只显示当前页面（所属模块可从菜单抽屉看到） -->
      <a-breadcrumb-item v-for="(item, i) in breadcrumbs.slice(-1)" :key="i">
        <MenuIcon v-if="item.icon" :name="item.icon" />
        <span>{{ item.title }}</span>
      </a-breadcrumb-item>
    </a-breadcrumb>

    <div class="header-right">
      <a-tooltip v-if="configStore.demoMode" :title="$t('layout.demoTip')">
        <a-tag color="orange" :bordered="false" class="demo-tag">{{ $t('layout.demoMode') }}</a-tag>
      </a-tooltip>
      <MenuSearch />
      <MessageBell />
      <LangSwitch btn-class="header-btn" />
      <a-tooltip :title="appStore.dark ? $t('layout.toLight') : $t('layout.toDark')">
        <a-button type="text" class="header-btn" @click="appStore.toggleDark()">
          <template #icon>
            <ThemeIcon :dark="appStore.dark" />
          </template>
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
import { computed, h } from 'vue'
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
  UserOutlined
} from '@ant-design/icons-vue'
import MenuIcon from '@/components/MenuIcon.vue'
import MenuSearch from './MenuSearch.vue'
import MessageBell from './MessageBell.vue'
import LangSwitch from '@/components/LangSwitch.vue'
import ThemeIcon from '@/components/ThemeIcon.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import { useAppStore } from '@/store/app'
import { useConfigStore } from '@/store/config'
import { useUserStore } from '@/store/user'
import { buildBreadcrumbIndex } from '@/utils/menu'
import { isZh } from '@/locales'
import { HOME_PATH, useModuleNav } from '../useModuleNav'

defineProps({ collapsed: Boolean, mobile: Boolean })
const emit = defineEmits(['toggle'])

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const configStore = useConfigStore()
const userStore = useUserStore()

const { modules, activeModule, openModule } = useModuleNav()
const moduleItems = computed(() => [
  { key: HOME_PATH, label: t('route.dashboard'), icon: () => h(MenuIcon, { name: 'HomeOutlined' }) },
  // 顶部空间有限，「供应链管理」显示为「供应链」
  ...modules.value.map((m) => ({
    key: m.key,
    label: isZh() ? m.title.replace(/(.{2,})管理$/, '$1') : m.title, // i18n-ignore 中文模块名去掉「管理」后缀
    icon: m.icon ? () => h(MenuIcon, { name: m.icon }) : undefined
  }))
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
