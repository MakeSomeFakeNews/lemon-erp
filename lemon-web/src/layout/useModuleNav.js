import { computed, reactive, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { flattenPages } from '@/utils/menu'
import { joinPath } from '@/utils/path'

/**
 * 顶部模块导航：首页 + 一级目录（模块），侧边栏只显示当前模块的菜单。
 * 首页、个人中心等不属于任何模块的页面，侧边栏显示模块列表。
 */
export const HOME_PATH = '/dashboard'
// 每个模块最近访问的页面，切换回来时回到这里
const lastPage = reactive({})

export function useModuleNav() {
  const route = useRoute()
  const router = useRouter()
  const userStore = useUserStore()

  const modules = computed(() =>
    (userStore.menus || [])
      .filter((m) => m.visible === 1)
      .map((m) => {
        const path = joinPath('', m.path)
        const pages = m.type === 'C' ? [{ path, title: m.name, icon: m.icon }] : flattenPages(m.children || [], path)
        return { key: path, menu: m, title: m.name, icon: m.icon, pages }
      })
      .filter((m) => m.pages.length)
  )

  const routeModule = computed(() => modules.value.find((m) => m.pages.some((p) => p.path === route.path)))

  watch(
    () => route.path,
    (path) => routeModule.value && (lastPage[routeModule.value.key] = path),
    { immediate: true }
  )

  function openModule(key) {
    if (key === HOME_PATH) return router.push(HOME_PATH)
    const m = modules.value.find((x) => x.key === key)
    if (!m) return
    const target = lastPage[m.key] || m.pages[0]?.path
    if (target && target !== route.path) router.push(target)
  }

  return { modules, activeModule: routeModule, openModule }
}
