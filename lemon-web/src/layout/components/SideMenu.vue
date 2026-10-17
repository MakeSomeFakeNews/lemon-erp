<template>
  <div class="side-menu">
    <router-link to="/" class="logo">
      <AppLogo :size="28" class="logo-icon" />
      <transition name="logo-fade">
        <span v-show="!collapsed" class="logo-title">{{ configStore.systemName }}</span>
      </transition>
    </router-link>
    <div class="menu-scroll scroll-hover">
      <a-menu
        theme="light"
        mode="inline"
        :items="menuItems"
        :selected-keys="[route.path]"
        :open-keys="collapsed ? undefined : openKeys"
        @open-change="(keys) => (openKeys = keys)"
        @click="onClick"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, h, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppLogo from '@/components/AppLogo.vue'
import MenuIcon from '@/components/MenuIcon.vue'
import { t } from '@/locales'
import { useConfigStore } from '@/store/config'
import { useUserStore } from '@/store/user'
import { joinPath } from '@/utils/path'
import { HOME_PATH, useModuleNav } from '../useModuleNav'

/**
 * 桌面端只显示当前模块的菜单（模块在顶部切换）；移动端抽屉（full）显示完整菜单树。
 * 模块下的二级目录作为分组标题，不折叠。
 */
const configStore = useConfigStore()
const props = defineProps({ collapsed: Boolean, full: Boolean })
const emit = defineEmits(['navigate'])

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { modules, activeModule, openModule } = useModuleNav()

const icon = (name) => (name ? () => h(MenuIcon, { name }) : undefined)

function toItems(menus, parentPath = '', depth = 0) {
  return menus
    .filter((m) => m.visible === 1)
    .map((m) => {
      const fullPath = joinPath(parentPath, m.path)
      if (m.type !== 'M') return { key: fullPath, label: m.name, icon: icon(m.icon) }
      const children = toItems(m.children || [], fullPath, depth + 1)
      if (!children.length) return null
      // 模块内的目录作为分组；顶层目录（仅移动端完整菜单）作为可折叠的子菜单
      return depth === 0
        ? { key: fullPath, label: m.name, icon: icon(m.icon), children }
        : { type: 'group', key: `group-${m.id}`, label: m.name, children }
    })
    .filter(Boolean)
}

const MODULE_PREFIX = 'module:'
const menuItems = computed(() => {
  if (props.full) return [{ key: HOME_PATH, label: t('route.dashboard'), icon: icon('HomeOutlined') }, ...toItems(userStore.menus)]
  const m = activeModule.value?.menu
  // 不在任何模块中（首页、个人中心等）：显示模块列表，点击进入模块
  if (!m) return modules.value.map((x) => ({ key: MODULE_PREFIX + x.key, label: x.title, icon: icon(x.icon) }))
  const items = m.type === 'C' ? toItems([m]) : toItems(m.children || [], joinPath('', m.path), 1)
  // 收起时放不下分组标题，改为分隔线
  return props.collapsed
    ? items.flatMap((i) => (i.type === 'group' ? [{ type: 'divider', key: `d-${i.key}` }, ...i.children] : [i]))
    : items
})

// 移动端完整菜单：展开当前路由所在的模块
const contains = (items, key) => items.some((i) => i.key === key || (i.children && contains(i.children, key)))
const openKeys = ref([])
watch(
  [() => route.path, menuItems],
  ([path]) => {
    const parents = menuItems.value
      .filter((i) => i.children && i.type !== 'group' && contains(i.children, path))
      .map((i) => i.key)
    openKeys.value = [...new Set([...openKeys.value, ...parents])]
  },
  { immediate: true }
)

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
