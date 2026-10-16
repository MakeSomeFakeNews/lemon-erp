import request from '@/utils/request'

/** 可发起的流程（已启用且已发布） */
export const listStartModels = () => request.get('/workflow/start/models')
export const getStartModel = (modelKey) => request.get(`/workflow/start/models/${modelKey}`)
/** 发起流程，返回流程实例 ID */
export const startInstance = (data) => request.post('/workflow/instance', data)

export const pageMyInstance = (params) => request.get('/workflow/instance/mine', { params })
export const pageInstance = (params) => request.get('/workflow/instance', { params })
export const getInstance = (id) => request.get(`/workflow/instance/${id}`)
/** 发起人撤回 */
export const cancelInstance = (id, reason) => request.post(`/workflow/instance/${id}/cancel`, { reason })
/** 管理员终止 */
export const terminateInstance = (id, reason) => request.post(`/workflow/instance/${id}/terminate`, { reason })
