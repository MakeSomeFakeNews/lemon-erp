import request from '@/utils/request'

export const pageUser = (params) => request.get('/system/user', { params })
/** 只查内部用户（排除供应商 / 经销商门户账号）：选择部门负责人、部门成员时使用 */
export const pageInternalUser = (params) => pageUser({ ...params, internal: true })
export const getUser = (id) => request.get(`/system/user/${id}`)
export const addUser = (data) => request.post('/system/user', data)
export const updateUser = (id, data) => request.put(`/system/user/${id}`, data)
export const deleteUser = (ids) => request.delete(`/system/user/${[].concat(ids).join(',')}`)
export const resetUserPassword = (id, newPassword) => request.put(`/system/user/${id}/password`, { newPassword })
export const changeUserStatus = (id, status) => request.put(`/system/user/${id}/status`, { status })
export const changeUserDept = (userIds, deptId) => request.put('/system/user/dept', { userIds, deptId })
