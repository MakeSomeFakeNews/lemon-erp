import request from '@/utils/request'

/** 生产工单 */
export const pageWo = (params) => request.get('/mes/wo', { params })
export const getWo = (id) => request.get(`/mes/wo/${id}`)
export const addWo = (data) => request.post('/mes/wo', data)
export const updateWo = (id, data) => request.put(`/mes/wo/${id}`, data)
export const releaseWo = (id) => request.post(`/mes/wo/${id}/release`)
export const closeWo = (id) => request.post(`/mes/wo/${id}/close`)
export const deleteWo = (ids) => request.delete(`/mes/wo/${[].concat(ids).join(',')}`)
export const reportWo = (id, data) => request.post(`/mes/wo/${id}/report`, data)
export const pageReport = (params) => request.get('/mes/wo/reports', { params })
/** 改量：{ qty }，不少于已完工 / 已报工数量 */
export const changeWoQty = (id, qty) => request.put(`/mes/wo/${id}/qty`, { qty })
/** 拆分：{ qty, planStart, planEnd }，返回新工单 ID */
export const splitWo = (id, data) => request.post(`/mes/wo/${id}/split`, data)
/** 新建返工工单（草稿），返回工单 ID */
export const addReworkWo = (data, sourceWoId) => request.post('/mes/wo/rework', data, { params: { sourceWoId } })
/** kind：issue 领料 / return 退料 / finish 完工入库 / scrap 报废入库 */
export const addWoDoc = (id, kind, data) => request.post(`/mes/wo/${id}/${kind}`, data)
export const pageWoDoc = (params) => request.get('/mes/wo/docs', { params })
export const getWoDoc = (docId) => request.get(`/mes/wo/docs/${docId}`)

/** 补料申请 */
export const pageSupplement = (params) => request.get('/mes/supplement', { params })
export const getSupplement = (id) => request.get(`/mes/supplement/${id}`)
/** 返回申请 ID */
export const addSupplement = (data) => request.post('/mes/supplement', data)
export const cancelSupplement = (id) => request.post(`/mes/supplement/${id}/cancel`)
/** 发料，返回补料单号 */
export const issueSupplement = (id, data) => request.post(`/mes/supplement/${id}/issue`, data)
