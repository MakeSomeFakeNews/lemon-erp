import { joinPath } from './path'

/**
 * 建立「完整路径 → 菜单层级标题」索引，用于面包屑
 */
export function buildBreadcrumbIndex(menus, parentPath = '', parents = [], index = new Map()) {
  for (const m of menus) {
    const fullPath = joinPath(parentPath, m.path)
    const chain = [...parents, { title: m.name, icon: m.icon }]
    if (m.type === 'C') index.set(fullPath, chain)
    if (m.children?.length) buildBreadcrumbIndex(m.children, fullPath, chain, index)
  }
  return index
}

/** 拍平得到所有可访问的页面菜单（快捷入口等使用） */
export function flattenPages(menus, parentPath = '', result = []) {
  for (const m of menus) {
    const fullPath = joinPath(parentPath, m.path)
    if (m.type === 'C' && m.visible === 1) result.push({ path: fullPath, title: m.name, icon: m.icon })
    if (m.children?.length) flattenPages(m.children, fullPath, result)
  }
  return result
}
