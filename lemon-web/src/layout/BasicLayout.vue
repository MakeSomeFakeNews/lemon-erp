<template>
  <!-- 双栏布局的模块栏不是 a-layout-sider，要显式声明横向排列 -->
  <a-layout :class="['layout', `layout-${layout}`]" :has-sider="!isMobile && layout !== LayoutMode.TOP">
    <!-- 双栏布局：最左一列模块 -->
    <ModuleRail v-if="!isMobile && layout === LayoutMode.COLUMN" />
    <!-- 桌面端侧边栏：当前模块的菜单（顶部模块 + 侧边菜单、双栏）或完整的多级树（侧边菜单）；顶部菜单布局没有侧边栏 -->
    <a-layout-sider
      v-if="!isMobile && hasSider"
      :collapsed="collapsed"
      :trigger="null"
      collapsible
      :width="layout === LayoutMode.COLUMN ? 200 : 220"
      :collapsed-width="layout === LayoutMode.COLUMN ? 0 : 64"
      :class="['sider', 'app-sider', { 'sider-hidden': layout === LayoutMode.COLUMN && collapsed }]"
    >
      <SideMenu
        :collapsed="collapsed"
        :mode="layout === LayoutMode.SIDE ? 'tree' : 'module'"
        :hide-logo="layout === LayoutMode.COLUMN"
      />
    </a-layout-sider>
    <!-- 移动端抽屉菜单（不区分布局） -->
    <a-drawer
      v-if="isMobile"
      :open="drawerOpen"
      placement="left"
      :width="220"
      :closable="false"
      :body-style="{ padding: 0 }"
      class="app-sider"
      @close="drawerOpen = false"
    >
      <SideMenu mode="full" @navigate="drawerOpen = false" />
    </a-drawer>

    <a-layout class="main">
      <div class="main-header">
        <HeaderBar
          :collapsed="isMobile ? !drawerOpen : collapsed"
          :mobile="isMobile"
          :layout="layout"
          :show-toggle="isMobile || hasSider"
          @toggle="toggleMenu"
          @settings="settingsOpen = true"
        />
        <TabsBar v-if="appStore.settings.tabs" />
      </div>
      <div class="content-wrap">
        <a-layout-content class="content">
          <router-view v-slot="{ Component, route: r }">
            <transition name="fade-slide" mode="out-in" appear>
              <component :is="Component" v-if="!appStore.reloading" :key="r.path" />
            </transition>
          </router-view>
        </a-layout-content>
        <!-- 子页（SubPage）挂载点：覆盖内容区 -->
        <div id="subpage-host" class="subpage-host" />
      </div>
    </a-layout>
    <SettingDrawer v-model:open="settingsOpen" />
    <Watermark />
  </a-layout>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import Watermark from '@/components/Watermark.vue'
import { useAppStore } from '@/store/app'
import { LayoutMode } from '@/utils/uiSettings'
import SideMenu from './components/SideMenu.vue'
import HeaderBar from './components/HeaderBar.vue'
import TabsBar from './components/TabsBar.vue'
import ModuleRail from './components/ModuleRail.vue'
import SettingDrawer from './components/settings/SettingDrawer.vue'
import { useModuleNav } from './useModuleNav'

const appStore = useAppStore()
const { activeModule } = useModuleNav()
const settingsOpen = ref(false)

// 小于 768px 时侧边栏改为抽屉；768 ~ 1199px（平板）默认收起，展开只在本次有效，不改变保存的偏好
const mobileMedia = window.matchMedia('(max-width: 767px)')
const tabletMedia = window.matchMedia('(max-width: 1199px)')
const isMobile = ref(mobileMedia.matches)
const isTablet = ref(tabletMedia.matches)
const tabletExpanded = ref(false)
const drawerOpen = ref(false)
const collapsed = computed(() => (isTablet.value ? !tabletExpanded.value : appStore.collapsed))
const onMobile = (e) => (isMobile.value = e.matches)
const onTablet = (e) => {
  isTablet.value = e.matches
  tabletExpanded.value = false
}
onMounted(() => {
  mobileMedia.addEventListener('change', onMobile)
  tabletMedia.addEventListener('change', onTablet)
})
onBeforeUnmount(() => {
  mobileMedia.removeEventListener('change', onMobile)
  tabletMedia.removeEventListener('change', onTablet)
})

const layout = computed(() => appStore.layout)
// 顶部菜单布局没有侧边栏；双栏布局在首页等不属于模块的页面只显示模块栏
const hasSider = computed(() =>
  layout.value === LayoutMode.TOP ? false : layout.value === LayoutMode.COLUMN ? !!activeModule.value : true
)

function toggleMenu() {
  if (isMobile.value) drawerOpen.value = !drawerOpen.value
  else if (isTablet.value) tabletExpanded.value = !tabletExpanded.value
  else appStore.toggleCollapsed()
}
</script>

<style scoped>
/* 整体固定一屏高度：页头不动，只有内容区滚动 */
.layout {
  height: 100vh;
  overflow: hidden;
}
.sider {
  height: 100vh;
  border-right: 1px solid var(--app-border);
  z-index: 10;
}
/* 双栏布局收起第二栏时宽度为 0，不显示分隔线 */
.sider.sider-hidden {
  border-right: none;
}
.main {
  min-width: 0;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--app-bg);
}
.main-header {
  flex: none;
  z-index: 9;
}
.content-wrap {
  position: relative;
  flex: 1;
  min-height: 0;
}
.subpage-host {
  position: absolute;
  inset: 0;
  z-index: 5;
  pointer-events: none;
}
.content {
  height: 100%;
  min-width: 0;
  padding: 16px;
  overflow: auto;
}
@media (max-width: 575px) {
  .content {
    padding: 12px;
  }
}
</style>
