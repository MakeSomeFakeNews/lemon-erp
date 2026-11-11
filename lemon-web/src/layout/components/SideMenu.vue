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
import { useConfigStore } from '@/store/config'
import { useUserStore } from '@/store/user'
import { joinPath } from '@/utils/path'
import { useModuleNav } from '../useModuleNav'
import { homeItem, menuIcon, parentKeys, toMenuItems } from '../menuItems'

import { MenuType } from '@/constants/enums'
/**
 * 侧边菜单，mode：
 * - module 只显示当前模块的菜单（顶部模块 + 侧边菜单、双栏布局），模块下的二级目录作为分组标题，不折叠；
 *          不在任何模块中（首页、个人中心等）时显示模块列表
 * - tree   全部模块、分组、页面的多级树（侧边菜单布局），模块和分组都可折叠，展开一个模块时收起其他模块
 * - full   完整菜单（移动端抽屉），模块可折叠，分组为标题
 */
const configStore = useConfigStore()
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

const MODULE_PREFIX = 'module:'
const menuItems = computed(() => {
  if (props.mode !== 'module') {
    return [homeItem(), ...toMenuItems(userStore.menus, { groups: props.mode === 'tree' ? 'submenu' : 'group' })]
  }
  const m = activeModule.value?.menu
  // 不在任何模块中（首页、个人中心等）：显示模块列表，点击进入模块
  if (!m) return modules.value.map((x) => ({ key: MODULE_PREFIX + x.key, label: x.title, icon: menuIcon(x.icon) }))
  const items = m.type === MenuType.C ? toMenuItems([m]) : toMenuItems(m.children, { parentPath: joinPath('', m.path), depth: 1 })
  // 收起时放不下分组标题，改为分隔线
  return props.collapsed
    ? items.flatMap((i) => (i.type === 'group' ? [{ type: 'divider', key: `d-${i.key}` }, ...i.children] : [i]))
    : items
})

// 展开当前路由所在的模块（和分组）；多级树同时只展开一个模块，避免整棵树过长
const topKeys = computed(() => new Set(menuItems.value.filter((i) => i.children).map((i) => i.key)))
const openKeys = ref([])
watch(
  [() => route.path, menuItems],
  ([path]) => {
    const parents = parentKeys(menuItems.value, path) || []
    const keep = props.mode === 'tree' && parents.length ? openKeys.value.filter((k) => !topKeys.value.has(k) || parents.includes(k)) : openKeys.value
    openKeys.value = [...new Set([...keep, ...parents])]
  },
  { immediate: true }
)

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
  const opened = keys.find((k) => !openKeys.value.includes(k))
  openKeys.value = props.mode === 'tree' && opened && topKeys.value.has(opened)
    ? keys.filter((k) => !topKeys.value.has(k) || k === opened)
    : keys
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
