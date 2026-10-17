import request from '@/utils/request'

/** 第三方集成配置：[{ platform, enabled, corpId, appKey, agentId, homeUrl, appSecretSet, ..., callbackUrl, entryUrl }]，密钥不返回 */
export const listImConfig = () => request.get('/system/im/config')
/** 保存配置：密钥为空表示不修改 */
export const saveImConfig = (data) => request.put('/system/im/config', data)
/** 校验应用凭证，已绑定时给自己发测试消息，返回提示文字 */
export const verifyIm = (platform) => request.post(`/system/im/${platform}/verify`)
export const testImRobot = (platform) => request.post(`/system/im/${platform}/robot-test`)
/** 同步通讯录：{ memberTotal, alreadyBound, matched, created, unmatchedCount, unmatched, deptCreated, warning } */
export const syncImContacts = (platform) => request.post(`/system/im/${platform}/sync`, null, { timeout: 180000 })
/** 成员绑定列表：{ platform, bound, keyword, current, size } */
export const listImBindings = (params) => request.get('/system/im/bindings', { params })
export const bindImUser = (data) => request.post('/system/im/bindings', data)
export const unbindImUser = (id) => request.delete(`/system/im/bindings/${id}`)

/** 个人中心：已启用的平台及本人的绑定 */
export const listMyIm = () => request.get('/system/im/mine')
export const getImBindUrl = (platform) => request.get(`/system/im/mine/${platform}/bind-url`)
export const unbindMyIm = (platform) => request.delete(`/system/im/mine/${platform}`)

/** 登录页的第三方登录方式（免登录） */
export const listImProviders = (tenantName) => request.get('/auth/im/providers', { params: { tenantName } })
/** 授权页地址：{ tenantName | tenantId, inApp, redirect } */
export const getImAuthorizeUrl = (platform, params) => request.get(`/auth/im/${platform}/authorize`, { params })
/** 授权回调：{ code, state } → { platform, bind, token, passwordExpired, redirect } */
export const imCallback = (data) => request.post('/auth/im/callback', data)
