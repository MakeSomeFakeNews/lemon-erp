<template>
  <div class="tabs-bar">
    <a-tabs
      :active-key="route.path"
      type="editable-card"
      size="small"
      hide-add
      class="tabs"
      @change="onChange"
      @edit="(key) => close(key)"
    >
      <a-tab-pane v-for="tab in appStore.tabs" :key="tab.path" :closable="!tab.affix">
        <template #tab>
          <a-dropdown :trigger="['contextmenu']">
            <span class="tab-label">
              <MenuIcon v-if="tab.icon" :name="tab.icon" />
              {{ tab.title }}
            </span>
            <template #overlay>
              <a-menu :items="menuItems(tab)" @click="({ key }) => handle(key, tab)" />
            </template>
          </a-dropdown>
        </template>
      </a-tab-pane>
      <!-- 标签栏右侧：刷新当前页、标签操作 -->
      <template #rightExtra>
        <div class="tabs-extra">
          <a-tooltip :title="$t('components.layout.reloadPage')">
            <a-button type="text" size="small" @click="appStore.reloadPage()">
              <template #icon><ReloadOutlined :spin="appStore.reloading" /></template>
            </a-button>
          </a-tooltip>
          <a-dropdown placement="bottomRight">
            <a-button type="text" size="small">
              <template #icon><DownOutlined /></template>
            </a-button>
            <template #overlay>
              <a-menu :items="menuItems(currentTab)" @click="({ key }) => handle(key, currentTab)" />
            </template>
          </a-dropdown>
        </div>
      </template>
    </a-tabs>
  </div>
</template>

<script setup>
import { computed, h, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  CloseOutlined,
  ColumnWidthOutlined,
  DownOutlined,
  MinusOutlined,
  ReloadOutlined,
  VerticalLeftOutlined,
  VerticalRightOutlined
} from '@ant-design/icons-vue'
import MenuIcon from '@/components/MenuIcon.vue'
import { t } from '@/locales'
import { useAppStore, HOME_TAB } from '@/store/app'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()

// 进入页面时加入标签（登录页、404 不加入）
watch(
  () => route.fullPath,
  () => {
    if (route.name !== 'NotFound') appStore.addTab(route)
  },
  { immediate: true }
)

const currentTab = computed(() => appStore.tabs.find((tb) => tb.path === route.path) || HOME_TAB)

function menuItems(tab) {
  const idx = appStore.tabs.findIndex((tb) => tb.path === tab.path)
  const closable = appStore.tabs.filter((tb) => !tb.affix)
  return [
    { key: 'reload', label: t('common.action.refresh'), icon: () => h(ReloadOutlined), disabled: tab.path !== route.path },
    { key: 'close', label: t('common.action.close'), icon: () => h(CloseOutlined), disabled: !!tab.affix },
    { type: 'divider' },
    { key: 'others', label: t('components.layout.closeOthers'), icon: () => h(ColumnWidthOutlined), disabled: closable.every((tb) => tb.path === tab.path) },
    { key: 'left', label: t('components.layout.closeLeft'), icon: () => h(VerticalRightOutlined), disabled: !appStore.tabs.slice(0, idx).some((tb) => !tb.affix) },
    { key: 'right', label: t('components.layout.closeRight'), icon: () => h(VerticalLeftOutlined), disabled: !appStore.tabs.slice(idx + 1).some((tb) => !tb.affix) },
    { key: 'all', label: t('components.layout.closeAll'), icon: () => h(MinusOutlined), disabled: !closable.length }
  ]
}

function onChange(path) {
  const tab = appStore.tabs.find((tb) => tb.path === path)
  router.push(tab?.fullPath || path)
}

function close(path) {
  const next = appStore.closeTab(path, route.path)
  if (next) router.push(next)
}

function handle(key, tab) {
  if (key === 'reload') return appStore.reloadPage()
  if (key === 'close') return close(tab.path)
  appStore.closeTabs(key, tab.path)
  // 当前页被关掉时，跳到操作的标签或首页
  if (!appStore.tabs.some((tb) => tb.path === route.path)) {
    const target = appStore.tabs.find((tb) => tb.path === tab.path) || HOME_TAB
    router.push(target.fullPath)
  } else if (tab.path !== route.path && key === 'others') {
    router.push(tab.fullPath)
  }
}
</script>

<style scoped>
.tabs-bar {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px 0;
  background: var(--app-header);
  border-bottom: 1px solid var(--app-border);
}
.tabs {
  flex: 1;
  min-width: 0;
}
.tabs :deep(.ant-tabs-nav) {
  margin: 0;
}
.tabs :deep(.ant-tabs-nav::before) {
  border-bottom: none;
}
.tabs :deep(.ant-tabs-tab) {
  border-radius: 6px 6px 0 0 !important;
}
.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  user-select: none;
}
.tab-label :deep(.anticon) {
  margin: 0;
}
.tabs-extra {
  display: flex;
  align-items: center;
  gap: 2px;
  margin-left: 4px;
  padding-bottom: 4px;
}
</style>
