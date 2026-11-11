<template>
  <a-menu
    mode="horizontal"
    class="top-menu"
    :items="items"
    :selected-keys="[route.path]"
    @click="onClick"
  />
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { homeItem, shortModuleTitle, toMenuItems } from '../menuItems'

/**
 * 顶部菜单布局：全部菜单显示为水平菜单，鼠标移到模块上展开下拉面板，分组按列排列（style.css 的 .top-menu-popup / .top-menu-cols-N），
 * 放不下的模块收进「…」
 */
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const items = computed(() => [
  homeItem(),
  ...toMenuItems(userStore.menus).map((m) => ({
    ...m,
    label: shortModuleTitle(m.label),
    ...(m.children && { popupClassName: popupClass(m.children) })
  }))
])

// 有分组的模块排成多列（最多 4 列），只有页面的模块用普通下拉。分组不跨列，列数按总行数和最长的分组估算，
// 避免浏览器平衡各列高度后最后一列空着
function popupClass(children) {
  const sizes = children.map((c) => (c.type === 'group' ? c.children.length + 1 : 1))
  if (!children.some((c) => c.type === 'group') || sizes.length < 2) return 'top-menu-popup'
  const total = sizes.reduce((a, b) => a + b, 0)
  const cols = Math.min(4, sizes.length, Math.ceil(total / Math.max(...sizes, total / 4)))
  return cols > 1 ? `top-menu-popup top-menu-cols-${cols}` : 'top-menu-popup'
}

function onClick({ key }) {
  if (/^https?:/.test(key)) window.open(key, '_blank')
  else router.push(key)
}
</script>

<style scoped>
.top-menu {
  flex: 1;
  min-width: 0;
  height: 55px;
  line-height: 55px;
  border-bottom: none;
  background: transparent;
}
.top-menu :deep(.ant-menu-item),
.top-menu :deep(.ant-menu-submenu) {
  padding-inline: 12px;
}
</style>
