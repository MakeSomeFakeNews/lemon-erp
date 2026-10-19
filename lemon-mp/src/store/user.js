import { reactive } from 'vue'
import { getInfo } from '@/api/auth'
import { removeToken } from '@/utils/auth'
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

/** 有任一权限即可；不传时为 true */
export function hasPerm(perm) {
  if (!perm) return true
  const list = Array.isArray(perm) ? perm : [perm]
  return userState.perms.includes(ALL_PERMISSION) || list.some((p) => userState.perms.includes(p))
}
