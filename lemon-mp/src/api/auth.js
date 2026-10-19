import { get, post, put } from '@/utils/request'

export const getConfig = () => get('/auth/config', null, { silent: true })
export const getCaptcha = () => get('/auth/captcha')
export const getInfo = () => get('/auth/info')
export const logout = () => post('/auth/logout', null, { silent: true })

/** 微信登录：code 为 wx.login 的结果；switchAccount 使用其他账号登录（不自动登录） */
export const miniappLogin = (code, switchAccount = false) => post('/auth/miniapp/login', { code, switchAccount })
export const miniappChoose = (key, tenantId) => post('/auth/miniapp/choose', { key, tenantId })
/** 账号密码登录并绑定当前微信：{ key, username, password, tenantName, captchaId, captchaCode } */
export const miniappBind = (data) => post('/auth/miniapp/bind', data)
export const miniappUnbind = () => post('/auth/miniapp/unbind')
/** H5 调试时没有微信登录，直接用账号密码登录 */
export const passwordLogin = (data) => post('/auth/login', data)

/** 个人资料：{ user, deptName, roleNames } */
export const getProfile = () => get('/profile', null, { silent: true })
export const changePassword = (data) => put('/profile/password', data)
