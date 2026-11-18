<template>
  <div class="side-menu">
    <router-link v-if="!hideLogo" to="/" class="logo">
      <AppLogo :size="28" class="logo-icon" />
      <transition name="logo-fade">
        <span v-show="!collapsed" class="logo-title">{{ configStore.systemName }}</span>
      </transition>
    </router-link>
    <!-- 双栏布局的第二栏：显示当前模块名称 -->
    <div v-else-if="activeModule" class="module-title">{{ activeModule.title }}</div>
    <div ref="scrollRef" class="menu-scroll scroll-hover">
      <a-menu
        theme="light"
        mode="inline"
        :items="menuItems"
        :inline-indent="indent"
        :selected-keys="[route.path]"
        :open-keys="collapsed ? undefined : openKeys"
        @open-change="onOpenChange"
        @click="onClick"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppLogo from '@/components/AppLogo.vue'
import { useAppStore } from '@/store/app'
import { useConfigStore } from '@/store/config'
import { useUserStore } from '@/store/user'
import { joinPath } from '@/utils/path'
import { load, save } from '@/utils/storage'
import { SideGroups } from '@/utils/uiSettings'
import { useModuleNav } from '../useModuleNav'
import { homeItem, menuIcon, parentKeys, siblingKeys, toMenuItems } from '../menuItems'

import { MenuType } from '@/constants/enums'
/**
 * 侧边菜单，mode：
 * - module 只显示当前模块的菜单（顶部模块 + 侧边菜单、双栏布局）；不在任何模块中（首页、个人中心等）时显示模块列表
 * - tree   全部模块、分组、页面的多级树（侧边菜单布局），展开一个模块时收起其他模块
 * - full   完整菜单（移动端抽屉），模块可折叠
 * 模块下的二级目录（分组）按界面设置「侧边栏分组」显示（issue #133）：可折叠 / 手风琴为子菜单，默认只展开当前页面所在的分组，
 * 用户展开、收起的状态记在本浏览器；始终展开为分组标题。收起成图标时分组改为分隔线（module 模式）
 */
const configStore = useConfigStore()
const appStore = useAppStore()
const props = defineProps({
  collapsed: Boolean,
  mode: { type: String, default: 'module' },
  /** 不显示 Logo（双栏布局的 Logo 在最左一列） */
  hideLogo: Boolean
})
const emit = defineEmits(['navigate'])

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { modules, activeModule, openModule } = useModuleNav()

const sideGroups = computed(() => appStore.settings.sideGroups)
const accordion = computed(() => sideGroups.value === SideGroups.ACCORDION)
// 收起成图标时（module 模式）放不下分组，按分组标题生成后再改为分隔线
const groups = computed(() => (sideGroups.value === SideGroups.EXPAND || (props.mode === 'module' && props.collapsed) ? 'group' : 'submenu'))

const MODULE_PREFIX = 'module:'
const menuItems = computed(() => {
  if (props.mode !== 'module') {
    // 多级树：模块是子菜单，分组按设置；移动端抽屉同样
    return [homeItem(), ...toMenuItems(userStore.menus, { groups: groups.value })]
  }
  const m = activeModule.value?.menu
  // 不在任何模块中（首页、个人中心等）：显示模块列表，点击进入模块
  if (!m) return modules.value.map((x) => ({ key: MODULE_PREFIX + x.key, label: x.title, icon: menuIcon(x.icon) }))
  const items = m.type === MenuType.C
    ? toMenuItems([m])
    : toMenuItems(m.children, { parentPath: joinPath('', m.path), depth: 1, groups: groups.value })
  return props.collapsed
    ? items.flatMap((i) => (i.type === 'group' ? [{ type: 'divider', key: `d-${i.key}` }, ...i.children] : [i]))
    : items
})

// 有可折叠的子菜单时缩小缩进（侧边栏只有 220px，页面在分组下多缩进一级），没有时（如始终展开）与旧版相同
const isSubmenu = (i) => i.children && i.type !== 'group'
const hasNested = (items) => items.some((i) => isSubmenu(i) && i.children.some(isSubmenu))
const indent = computed(() => {
  if (props.collapsed) return 24
  return props.mode === 'module' ? (menuItems.value.some(isSubmenu) ? 16 : 24) : hasNested(menuItems.value) ? 16 : 24
})

/**
 * 展开状态：当前路由所在的模块和分组总是展开；用户展开、收起的分组记在本浏览器（各布局共用，分组 key 为菜单 ID）。
 * 多级树同时只展开一个模块，避免整棵树过长；手风琴时同一层只展开一个
 */
const OPEN_KEY = 'lemon_erp_menu_open'
const topKeys = computed(() => new Set(menuItems.value.filter(isSubmenu).map((i) => i.key)))
const stored = load(OPEN_KEY, [])
const openKeys = ref(Array.isArray(stored) ? stored.filter((k) => typeof k === 'string') : [])
/** 展开 key 时是否收起同层的其他项 */
const exclusive = (key) => accordion.value || (props.mode === 'tree' && topKeys.value.has(key))
/** 展开 keys 中的各项，按需收起它们的同层项 */
function openWith(keys, add) {
  let result = keys
  for (const k of add) {
    if (exclusive(k)) {
      const siblings = siblingKeys(menuItems.value, k) || []
      result = result.filter((x) => x === k || !siblings.includes(x))
    }
  }
  return [...new Set([...result, ...add])]
}
watch(
  [() => route.path, menuItems],
  ([path]) => (openKeys.value = openWith(openKeys.value, parentKeys(menuItems.value, path) || [])),
  { immediate: true }
)
watch(openKeys, (keys) => save(OPEN_KEY, keys))

// 当前页面不在可见范围内时（菜单较长、刚展开模块）滚动到可见位置，等子菜单展开动画结束
const scrollRef = ref()
let scrollTimer
watch(
  () => route.path,
  () => {
    clearTimeout(scrollTimer)
    scrollTimer = setTimeout(() => scrollRef.value?.querySelector('.ant-menu-item-selected')?.scrollIntoView({ block: 'nearest' }), 350)
  },
  { immediate: true }
)
onBeforeUnmount(() => clearTimeout(scrollTimer))

function onOpenChange(keys) {
  // 收起成图标时子菜单是悬停弹出，不改变展开状态
  if (props.collapsed) return
  const opened = keys.filter((k) => !openKeys.value.includes(k))
  openKeys.value = openWith(keys, opened)
}

function onClick({ key }) {
  if (key.startsWith(MODULE_PREFIX)) {
    openModule(key.slice(MODULE_PREFIX.length))
  } else if (/^https?:/.test(key)) {
    window.open(key, '_blank')
  } else {
    router.push(key)
  }
  emit('navigate')
}
</script>

<style scoped>
.side-menu {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.logo {
  height: 64px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 10px;
  /* 固定左边距：收起（64px）时 Logo 与菜单图标居中对齐，展开时位置不变，切换时不跳动 */
  padding: 0 17.5px;
  overflow: hidden;
}
.logo-icon {
  flex: none;
}
.logo-title {
  color: var(--app-text);
  font-size: 18px;
  line-height: 28px;
  font-weight: 600;
  letter-spacing: 0.5px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  min-width: 0;
}
.module-title {
  height: 56px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  padding: 0 20px;
  font-size: 15px;
  font-weight: 600;
  color: var(--app-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  border-bottom: 1px solid var(--app-border);
}
.menu-scroll {
  flex: 1;
  overflow-x: hidden;
  overflow-y: auto;
}
.logo-fade-enter-active {
  transition: opacity 0.3s 0.1s;
}
.logo-fade-enter-from {
  opacity: 0;
}
</style>
