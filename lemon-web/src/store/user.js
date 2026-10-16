import { defineStore } from 'pinia'
import * as authApi from '@/api/auth'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { useAppStore } from './app'
import { useDictStore } from './dict'

const ALL_PERMISSION = '*:*:*'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    user: null,
    roles: [],
    perms: [],
    /** SaaS 模式下当前租户的名称 */
    tenantName: null,
    menus: [],
    routesLoaded: false
  }),
  getters: {
    displayName: (state) => state.user?.nickname || state.user?.username || '',
    /** 当前用户是否为超级管理员（平台的 admin 角色；其他租户的 admin 是租户管理员，权限以套餐为限） */
    isAdmin: (state) => state.perms.includes(ALL_PERMISSION)
  },
  actions: {
    /** @returns {Promise<boolean>} 是否需要修改密码（初始密码、被重置或已过期） */
    async login(form) {
      const { token, passwordExpired } = await authApi.login(form)
      setToken(token)
      this.token = token
      return passwordExpired
    },
    async loadUser() {
      const [info, menus] = await Promise.all([authApi.getInfo(), authApi.getMenus()])
      this.user = info.user
      this.roles = info.roles
      this.perms = info.perms
      this.tenantName = info.tenantName
      this.menus = menus
    },
    async logout() {
      try {
        await authApi.logout()
      } finally {
        this.reset()
        useAppStore().resetTabs()
      }
    },
    reset() {
      removeToken()
      useDictStore().clear()
      this.$reset()
      this.token = null
    },
    hasPerm(perm) {
      if (!perm) return true
      const list = Array.isArray(perm) ? perm : [perm]
      return this.perms.includes(ALL_PERMISSION) || list.some((p) => this.perms.includes(p))
    }
  }
})
