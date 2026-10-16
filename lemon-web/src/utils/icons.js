import { shallowRef } from 'vue'

/**
 * 全量图标体积很大，只在需要按名称渲染（动态菜单、图标选择器）时异步加载，单独成 chunk
 */
export const iconModule = shallowRef(null)
let loading

export function loadIcons() {
  if (!loading) {
    loading = import('@ant-design/icons-vue').then((mod) => {
      iconModule.value = mod
      return mod
    })
  }
  return loading
}

/** 图标选择器可选的线框图标名称 */
export function outlinedIconNames(mod) {
  return Object.keys(mod).filter((name) => name.endsWith('Outlined'))
}
