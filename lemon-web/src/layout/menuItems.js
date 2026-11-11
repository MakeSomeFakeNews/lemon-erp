import { h } from 'vue'
import MenuIcon from '@/components/MenuIcon.vue'
import { isZh, t } from '@/locales'
import { joinPath } from '@/utils/path'
import { MenuType } from '@/constants/enums'
import { HOME_PATH } from './useModuleNav'

/**
 * 把菜单树转成 a-menu 的 items（侧边栏、顶部菜单、移动端抽屉共用）。
 * 一级目录是模块；模块下不填路由地址的二级目录是分组，按 groups 选择显示方式：
 * - 'group'   分组标题（不折叠），侧边栏当前模块、移动端完整菜单、顶部菜单的下拉面板使用
 * - 'submenu' 可折叠的子菜单，侧边菜单布局的多级树使用
 */
export const menuIcon = (name) => (name ? () => h(MenuIcon, { name }) : undefined)

export function toMenuItems(menus, { parentPath = '', depth = 0, groups = 'group' } = {}) {
  return (menus || [])
    .filter((m) => m.visible === 1)
    .map((m) => {
      const fullPath = joinPath(parentPath, m.path)
      if (m.type !== MenuType.M) return { key: fullPath, label: m.name, icon: menuIcon(m.icon) }
      const children = toMenuItems(m.children, { parentPath: fullPath, depth: depth + 1, groups })
      if (!children.length) return null
      if (depth === 0) return { key: fullPath, label: m.name, icon: menuIcon(m.icon), children }
      // 分组通常没有路由地址，完整路径与模块相同，用菜单 ID 作 key
      return groups === 'submenu'
        ? { key: `group-${m.id}`, label: m.name, icon: menuIcon(m.icon), children }
        : { type: 'group', key: `group-${m.id}`, label: m.name, children }
    })
    .filter(Boolean)
}

export const homeItem = () => ({ key: HOME_PATH, label: t('route.dashboard'), icon: menuIcon('HomeOutlined') })

/** 顶部空间有限，中文模块名去掉「管理」后缀（「供应链管理」显示为「供应链」） */
export const shortModuleTitle = (title) => (isZh() ? title.replace(/(.{2,})管理$/, '$1') : title) // i18n-ignore 中文模块名去掉「管理」后缀

/** items 中包含 key 的各级父项 key（用于展开当前页面所在的模块和分组） */
export function parentKeys(items, key, parents = []) {
  for (const item of items) {
    if (item.key === key) return parents
    if (item.children) {
      const found = parentKeys(item.children, key, item.type === 'group' ? parents : [...parents, item.key])
      if (found) return found
    }
  }
  return null
}
