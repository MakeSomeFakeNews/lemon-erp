import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

/** 生产计划 */
export const pagePlan = (params) => request.get('/mes/plan', { params })
export const addPlan = (data) => request.post('/mes/plan', data)
export const updatePlan = (id, data) => request.put(`/mes/plan/${id}`, data)
export const confirmPlan = (id) => request.post(`/mes/plan/confirm/${ids(id)}`)
export const closePlan = (id) => request.post(`/mes/plan/${id}/close`)
/** 计划转工单，返回草稿工单 ID */
export const planToWo = (id, data) => request.post(`/mes/plan/${id}/to-wo`, data)
export const deletePlan = (id) => request.delete(`/mes/plan/${ids(id)}`)
/** 由销售订单生成，返回生成的计划数 */
export const planFromSales = (orderId) => request.post(`/mes/plan/from-sales/${orderId}`)

/** MRP */
export const pageMrpRun = (params) => request.get('/mes/mrp', { params })
export const mrpResults = (runId) => request.get(`/mes/mrp/${runId}/results`)
/** 返回运算 ID */
export const runMrp = (data) => request.post('/mes/mrp/run', data)
export const updateMrpResult = (id, data) => request.put(`/mes/mrp/result/${id}`, data)
/** 返回生成的单据号 */
export const releaseMrp = (data) => request.post('/mes/mrp/release', data)
/** 销售预测：MRP 可选择计入 */
export const pageForecast = (params) => request.get('/mes/forecast', { params })
export const saveForecast = (data) => request.post('/mes/forecast', data)
export const deleteForecast = (ids) => request.delete(`/mes/forecast/${[].concat(ids).join(',')}`)
