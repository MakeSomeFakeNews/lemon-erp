<template>
  <a-layout class="layout">
    <!-- 桌面端侧边栏 -->
    <a-layout-sider
      v-if="!isMobile"
      :collapsed="collapsed"
      :trigger="null"
      collapsible
      :width="220"
      :collapsed-width="64"
      class="sider app-sider"
    >
      <SideMenu :collapsed="collapsed" />
    </a-layout-sider>
    <!-- 移动端抽屉菜单 -->
    <a-drawer
      v-else
      :open="drawerOpen"
      placement="left"
      :width="220"
      :closable="false"
      :body-style="{ padding: 0 }"
      class="app-sider"
      @close="drawerOpen = false"
    >
      <SideMenu full @navigate="drawerOpen = false" />
    </a-drawer>

    <a-layout class="main">
      <div class="main-header">
        <HeaderBar :collapsed="isMobile ? !drawerOpen : collapsed" :mobile="isMobile" @toggle="toggleMenu" />
        <TabsBar />
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
    <Watermark />
  </a-layout>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import Watermark from '@/components/Watermark.vue'
import { useAppStore } from '@/store/app'
import SideMenu from './components/SideMenu.vue'
import HeaderBar from './components/HeaderBar.vue'
import TabsBar from './components/TabsBar.vue'

const appStore = useAppStore()

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
