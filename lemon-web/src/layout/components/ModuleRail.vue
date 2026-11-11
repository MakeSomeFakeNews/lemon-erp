<template>
  <nav :class="['module-rail', { 'rail-en': !zh }]">
    <router-link to="/" class="rail-logo">
      <AppLogo :size="30" />
    </router-link>
    <div class="rail-list scroll-hover">
      <a-tooltip v-for="item in items" :key="item.key" :title="item.title" placement="right" :mouse-enter-delay="0.6">
        <button type="button" :class="['rail-item', { active: item.key === activeKey }]" @click="openModule(item.key)">
          <MenuIcon :name="item.icon || 'AppstoreOutlined'" class="rail-icon" />
          <span class="rail-text">{{ item.short }}</span>
        </button>
      </a-tooltip>
    </div>
  </nav>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppLogo from '@/components/AppLogo.vue'
import MenuIcon from '@/components/MenuIcon.vue'
import { isZh, t } from '@/locales'
import { HOME_PATH, useModuleNav } from '../useModuleNav'
import { shortModuleTitle } from '../menuItems'

/** 双栏布局的最左一列：首页和各模块的图标、名称，点击进入模块（回到该模块上次访问的页面） */
const route = useRoute()
const { modules, activeModule, openModule } = useModuleNav()
const zh = isZh()

const items = computed(() => [
  { key: HOME_PATH, title: t('route.dashboard'), short: t('route.dashboard'), icon: 'HomeOutlined' },
  ...modules.value.map((m) => ({ key: m.key, title: m.title, short: shortModuleTitle(m.title), icon: m.icon }))
])
const activeKey = computed(() => activeModule.value?.key || (route.path === HOME_PATH ? HOME_PATH : null))
</script>

<style scoped>
.module-rail {
  width: 92px;
  flex: none;
  height: 100vh;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--app-border);
  background: var(--app-sider);
  z-index: 11;
}
.rail-logo {
  height: 56px;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
}
.rail-list {
  flex: 1;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 4px 6px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.rail-item {
  /* 列表超出一屏时滚动，不压缩各项 */
  flex: none;
  width: 100%;
  min-height: 56px;
  padding: 8px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--app-sider-text);
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}
.rail-item:hover {
  background: var(--app-hover);
  color: var(--app-text);
}
.rail-item.active {
  background: var(--app-primary-bg);
  color: var(--app-primary);
  font-weight: 500;
}
.rail-icon {
  font-size: 18px;
}
/* 最多两行，全称见悬浮提示 */
.rail-text {
  display: block;
  max-width: 100%;
  max-height: 2.6em;
  font-size: 12px;
  line-height: 1.3;
  text-align: center;
  overflow: hidden;
  overflow-wrap: break-word;
}
/* 英文名称较长（如 Manufacturing），缩小字号以放进一行 */
.rail-en .rail-text {
  font-size: 11px;
}
</style>
