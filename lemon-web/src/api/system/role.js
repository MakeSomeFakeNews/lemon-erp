import request from '@/utils/request'

export const roleOptions = () => request.get('/system/role/options')
export const getRole = (id) => request.get(`/system/role/${id}`)
export const addRole = (data) => request.post('/system/role', data)
export const updateRole = (id, data) => request.put(`/system/role/${id}`, data)
export const deleteRole = (ids) => request.delete(`/system/role/${[].concat(ids).join(',')}`)
export const changeRoleStatus = (id, status) => request.put(`/system/role/${id}/status`, { status })
export const assignRoleDataScope = (id, data) => request.put(`/system/role/${id}/data-scope`, data)
export const listRoles = (params) => request.get('/system/role/list', { params })
/** params.assigned：true 已分配的成员，false 可添加的用户 */
export const pageRoleUsers = (id, params) => request.get(`/system/role/${id}/users`, { params })
export const addRoleUsers = (id, userIds) => request.post(`/system/role/${id}/users`, { userIds })
export const removeRoleUsers = (id, userIds) =>
  request.delete(`/system/role/${id}/users/${[].concat(userIds).join(',')}`)
