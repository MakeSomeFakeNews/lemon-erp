import request from '@/utils/request'

/** 选择审批人 / 转交人时使用，任何登录用户可调用 */
export const pageIdentityUsers = (params) => request.get('/workflow/identity/users', { params })
export const listIdentityRoles = () => request.get('/workflow/identity/roles')
