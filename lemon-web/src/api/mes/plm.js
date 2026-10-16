import request from '@/utils/request'

/** 工程变更 */
export const pageEcn = (params) => request.get('/mes/ecn', { params })
export const getEcn = (id) => request.get(`/mes/ecn/${id}`)
export const ecnDiff = (id) => request.get(`/mes/ecn/${id}/diff`)
/** 新建：data { title, fromBomId, newVersion, reason, description, effectiveDate, syncDraftWo }，返回 ID */
export const addEcn = (data) => request.post('/mes/ecn', data)
export const updateEcn = (id, data) => request.put(`/mes/ecn/${id}`, data)
export const deleteEcn = (id) => request.delete(`/mes/ecn/${id}`)
/** 提交，返回是否进入审批 */
export const submitEcn = (id) => request.post(`/mes/ecn/${id}/submit`)
export const effectEcn = (id) => request.post(`/mes/ecn/${id}/effect`)
export const cancelEcn = (id) => request.post(`/mes/ecn/${id}/cancel`)

/** 技术文档 */
export const pageDoc = (params) => request.get('/mes/doc', { params })
export const getDoc = (id) => request.get(`/mes/doc/${id}`)
export const docVersions = (id) => request.get(`/mes/doc/${id}/versions`)
/** 物料的已发布文档 */
export const releasedDocs = (materialIds) => request.get('/mes/doc/released', { params: { materialIds: [].concat(materialIds).join(',') } })
export const addDoc = (data) => request.post('/mes/doc', data)
export const updateDoc = (id, data) => request.put(`/mes/doc/${id}`, data)
export const deleteDoc = (id) => request.delete(`/mes/doc/${id}`)
export const releaseDoc = (id) => request.post(`/mes/doc/${id}/release`)
/** 升版：data { version, ecnId, description }，返回新版本 ID */
export const reviseDoc = (id, data) => request.post(`/mes/doc/${id}/revise`, data)
