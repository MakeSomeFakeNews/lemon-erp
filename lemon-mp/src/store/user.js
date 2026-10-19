import { reactive } from 'vue'
import { getInfo } from '@/api/auth'
import { getToken, removeToken } from '@/utils/auth'
import { clearDict } from './dict'

const ALL_PERMISSION = '*:*:*'

/** 当前用户（/auth/info）：用户、角色、权限标识、租户名称 */
export const userState = reactive({
  loaded: false,
  user: null,
  roles: [],
  perms: [],
  tenantName: null
})

let loading = null

/**
 * 确保已加载当前用户（有令牌时）：小程序启动、从链接直接打开子页面时用户信息可能还没加载，
 * 需要按权限决定加载什么数据的页面先 await；并发调用共用一次请求，失败时不抛出（由请求层处理 401）
 */
export function ensureUser() {
  if (userState.loaded || !getToken()) return Promise.resolve()
  loading ||= loadUser().finally(() => (loading = null))
  return loading.then(
    () => {},
    () => {}
  )
}

export async function loadUser() {
  const info = await getInfo()
  Object.assign(userState, { loaded: true, user: info.user, roles: info.roles || [], perms: info.perms || [], tenantName: info.tenantName })
  return info
}

export function resetUser() {
  removeToken()
  clearDict()
  Object.assign(userState, { loaded: false, user: null, roles: [], perms: [], tenantName: null })
}

/** 有任一权限即可；不传时为 true。读取的是响应式状态，在 computed / 模板中使用时用户信息加载后会自动更新 */
export function hasPerm(perm) {
  if (!perm) return true
  const list = Array.isArray(perm) ? perm : [perm]
  return userState.perms.includes(ALL_PERMISSION) || list.some((p) => userState.perms.includes(p))
}
