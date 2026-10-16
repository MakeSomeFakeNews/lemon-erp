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
      <MenuSearch />
      <MessageBell />
      <a-tooltip :title="appStore.dark ? '切换到亮色' : '切换到暗色'">
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
              个人中心
            </a-menu-item>
            <a-menu-item key="messages" @click="router.push('/messages')">
              <template #icon><BellOutlined /></template>
              我的消息
            </a-menu-item>
            <a-menu-divider />
            <a-menu-item key="logout" danger @click="confirmLogout">
              <template #icon><LogoutOutlined /></template>
              退出登录
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
import ThemeIcon from '@/components/ThemeIcon.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import { useAppStore } from '@/store/app'
import { useUserStore } from '@/store/user'
import { buildBreadcrumbIndex } from '@/utils/menu'
import { HOME_PATH, useModuleNav } from '../useModuleNav'

defineProps({ collapsed: Boolean, mobile: Boolean })
const emit = defineEmits(['toggle'])

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()

const { modules, activeModule, openModule } = useModuleNav()
const moduleItems = computed(() => [
  { key: HOME_PATH, label: '首页', icon: () => h(MenuIcon, { name: 'HomeOutlined' }) },
  // 顶部空间有限，「供应链管理」显示为「供应链」
  ...modules.value.map((m) => ({
    key: m.key,
    label: m.title.replace(/(.{2,})管理$/, '$1'),
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
    title: '退出登录',
    content: '确定要退出当前账号吗？',
    okText: '退出',
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
