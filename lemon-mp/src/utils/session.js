import { getToken, setToken } from './auth'
import { ensureUser, loadUser, userState } from '@/store/user'

/** 登录成功：保存令牌、加载用户；密码需要修改时进入修改密码页，否则回到工作台 */
export async function afterLogin(res) {
  setToken(res.token)
  await loadUser()
  if (res.passwordExpired) uni.reLaunch({ url: '/pages/mine/password?expired=1' })
  else uni.switchTab({ url: '/pages/index/index' })
}

/** 登录检查：没有令牌或令牌失效时进入登录页，返回是否已登录 */
export async function ensureLogin() {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return false
  }
  await ensureUser()
  return userState.loaded
}
