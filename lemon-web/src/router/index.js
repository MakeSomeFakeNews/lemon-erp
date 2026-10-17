import { createRouter, createWebHistory } from 'vue-router'
import { useConfigStore } from '@/store/config'
import { useUserStore } from '@/store/user'
import { getToken } from '@/utils/auth'
import { joinPath } from '@/utils/path'
import { t } from '@/locales'
import { useAppStore } from '@/store/app'
import BasicLayout from '@/layout/BasicLayout.vue'

export const LAYOUT_NAME = 'Layout'
/** 移动作业（手机 / PDA）：模块路由 /m 下的页面不使用后台布局，在全屏的移动端布局中显示 */
export const MOBILE_NAME = 'Mobile'
export const MOBILE_PATH = '/m'

const constantRoutes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: t('route.login') }
  },
  {
    path: '/im-callback',
    name: 'ImCallback',
    component: () => import('@/views/login/ImCallback.vue'),
    meta: { title: t('route.imLogin') }
  },
  {
    path: '/im-login',
    name: 'ImEntry',
    component: () => import('@/views/login/ImEntry.vue'),
    meta: { title: t('route.imLogin') }
  },
  {
    path: '/',
    name: LAYOUT_NAME,
    component: BasicLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: t('route.dashboard'), icon: 'HomeOutlined', affix: true }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: t('route.profile'), icon: 'UserOutlined' }
      },
      {
        path: 'messages',
        name: 'Messages',
        component: () => import('@/views/message/index.vue'),
        meta: { title: t('route.messages'), icon: 'BellOutlined' }
      }
    ]
  }
]

constantRoutes.push({
  path: MOBILE_PATH,
  name: MOBILE_NAME,
  component: () => import('@/layout/MobileLayout.vue'),
  meta: { title: t('route.mobile') },
  children: [{ path: '', name: 'MobileHome', component: () => import('@/views/mobile/home.vue'), meta: { title: t('route.mobile') } }]
})

/** 数据大屏：全屏显示，不使用后台布局；/screen-share 为投屏链接，免登录 */
export const SCREEN_SHARE_PATH = '/screen-share/'
constantRoutes.push(
  {
    path: '/screen/:key',
    name: 'Screen',
    component: () => import('@/views/screen/ScreenPage.vue'),
    meta: { title: t('route.screen') }
  },
  {
    path: `${SCREEN_SHARE_PATH}:token`,
    name: 'ScreenShare',
    component: () => import('@/views/screen/ShareScreenPage.vue'),
    meta: { title: t('route.screen') }
  }
)

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes,
  scrollBehavior: () => ({ top: 0 })
})

// 后端菜单 component 字段（如 system/user/index）对应 src/views 下的文件
const viewModules = import.meta.glob('../views/**/*.vue')

/**
 * 把后端菜单树拍平成 Layout 下的子路由；目录(M)只作为菜单分组，不生成路由
 */
function menusToRoutes(menus, parentPath = '', routes = []) {
  for (const menu of menus) {
    const fullPath = joinPath(parentPath, menu.path)
    if (menu.type === 'C' && menu.component) {
      const component = viewModules[`../views/${menu.component}.vue`]
      if (!component) {
        console.warn(`[router] 未找到组件 src/views/${menu.component}.vue`)
      }
      routes.push({
        path: fullPath,
        name: `Menu${menu.id}`,
        component: component || (() => import('@/views/error/404.vue')),
        meta: { title: menu.name, icon: menu.icon, perms: menu.perms }
      })
    }
    if (menu.children?.length) {
      menusToRoutes(menu.children, fullPath, routes)
    }
  }
  return routes
}

/** 免登录的页面：登录页、钉钉 / 企业微信 / 飞书的授权回调页和免登入口 */
const WHITE_LIST = ['/login', '/im-callback', '/im-login']
/** 已登录时也不加载菜单和路由的页面：回调页、免登入口自行处理登录（令牌可能已过期） */
const BARE_PAGES = ['/im-callback', '/im-login']
/** 门户账号没有首页工作台，进入所属门户：供应商 → 供应商门户，经销商 → 经销商门户 */
const portalHome = (user) => (user?.supplierId ? '/portal/order' : user?.customerId ? '/dealer/order' : null)

router.beforeEach(async (to) => {
  const userStore = useUserStore()
  // 公开配置（系统名称、验证码等）加载失败不影响进入页面，使用默认值
  await useConfigStore().load().catch(() => {})

  // 投屏链接免登录，不加载用户和菜单（电视上可能残留过期的登录令牌）
  if (to.path.startsWith(SCREEN_SHARE_PATH)) {
    return true
  }
  if (!getToken()) {
    return WHITE_LIST.includes(to.path) ? true : { path: '/login', query: { redirect: to.fullPath } }
  }
  if (BARE_PAGES.includes(to.path)) {
    return true
  }
  if (to.path === '/login') {
    return '/'
  }
  if (userStore.routesLoaded) {
    const home = to.path === '/dashboard' ? portalHome(userStore.user) : null
    return home || true
  }
  try {
    await userStore.loadUser()
    menusToRoutes(userStore.menus).forEach((route) =>
      router.addRoute(route.path.startsWith(`${MOBILE_PATH}/`) ? MOBILE_NAME : LAYOUT_NAME, route))
    router.addRoute(LAYOUT_NAME, {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('@/views/error/404.vue'),
      meta: { title: t('route.notFound') }
    })
    userStore.routesLoaded = true
    useAppStore().retitleTabs((path) => router.resolve(path).meta?.title)
    // 动态路由刚添加，需要重新匹配一次
    const home = to.path === '/dashboard' ? portalHome(userStore.user) : null
    if (home) {
      return { path: home, replace: true }
    }
    return { ...to, replace: true }
  } catch (e) {
    userStore.reset()
    return { path: '/login', query: { redirect: to.fullPath } }
  }
})

router.afterEach((to) => {
  const name = useConfigStore().systemName
  document.title = to.meta?.title ? `${to.meta.title} - ${name}` : name
})

export default router
