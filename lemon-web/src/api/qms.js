import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

/** 检验方案 */
export const pagePlan = (params) => request.get('/qms/plan', { params })
export const getPlan = (id) => request.get(`/qms/plan/${id}`)
export const addPlan = (data) => request.post('/qms/plan', data)
export const updatePlan = (id, data) => request.put(`/qms/plan/${id}`, data)
export const deletePlan = (id) => request.delete(`/qms/plan/${ids(id)}`)
/** 新建检验单时匹配方案并计算抽样数量，没有方案时返回 null */
export const matchPlan = (params) => request.get('/qms/plan/match', { params })

/** 不合格品处理 */
export const pageNcr = (params) => request.get('/qms/ncr', { params })
export const getNcr = (id) => request.get(`/qms/ncr/${id}`)
export const addNcr = (data) => request.post('/qms/ncr', data)
/** 评审处置，返回是否进入审批 */
export const reviewNcr = (id, data) => request.post(`/qms/ncr/${id}/review`, data)
export const cancelNcr = (id) => request.post(`/qms/ncr/${id}/cancel`)
export const deleteNcr = (id) => request.delete(`/qms/ncr/${id}`)

/** 纠正预防（8D） */
export const pageCapa = (params) => request.get('/qms/capa', { params })
export const getCapa = (id) => request.get(`/qms/capa/${id}`)
export const addCapa = (data) => request.post('/qms/capa', data)
export const updateCapa = (id, data) => request.put(`/qms/capa/${id}`, data)
export const saveCapaStep = (id, stepId, data) => request.put(`/qms/capa/${id}/steps/${stepId}`, data)
export const submitCapa = (id) => request.post(`/qms/capa/${id}/submit`)
export const verifyCapa = (id, data) => request.post(`/qms/capa/${id}/verify`, data)
export const deleteCapa = (id) => request.delete(`/qms/capa/${id}`)

/** 供应商索赔 */
export const pageClaim = (params) => request.get('/qms/claim', { params })
export const getClaim = (id) => request.get(`/qms/claim/${id}`)
export const addClaim = (data) => request.post('/qms/claim', data)
export const updateClaim = (id, data) => request.put(`/qms/claim/${id}`, data)
export const confirmClaim = (id) => request.post(`/qms/claim/${id}/confirm`)
export const cancelClaim = (id) => request.post(`/qms/claim/${id}/cancel`)
export const deleteClaim = (id) => request.delete(`/qms/claim/${id}`)

/** 质量分析（日期为空默认近 12 个月） */
export const qualityTrend = (params) => request.get('/qms/analysis/trend', { params })
export const qualityPareto = (params) => request.get('/qms/analysis/pareto', { params })
export const supplierQuality = (params) => request.get('/qms/analysis/supplier', { params })
export const spcItems = () => request.get('/qms/analysis/spc-items')
export const spcChart = (params) => request.get('/qms/analysis/spc', { params })
