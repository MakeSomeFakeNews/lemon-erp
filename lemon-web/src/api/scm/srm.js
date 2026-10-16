import request from '@/utils/request'

/** 采购合同 */
export const pageContract = (params) => request.get('/scm/contract', { params })
export const getContract = (id) => request.get(`/scm/contract/${id}`)
export const contractOrders = (id) => request.get(`/scm/contract/${id}/orders`)
/** 采购订单可引用的合同（含合同价）：{ supplierId, date } */
export const activeContracts = (params) => request.get('/scm/contract/active', { params })
export const addContract = (data) => request.post('/scm/contract', data)
export const updateContract = (id, data) => request.put(`/scm/contract/${id}`, data)
export const deleteContract = (id) => request.delete(`/scm/contract/${id}`)
/** 提交，返回是否进入审批 */
export const submitContract = (id) => request.post(`/scm/contract/${id}/submit`)
export const closeContract = (id, reason) => request.post(`/scm/contract/${id}/close`, { reason })

/** 招标 */
export const pageTender = (params) => request.get('/scm/tender', { params })
export const getTender = (id) => request.get(`/scm/tender/${id}`)
export const addTender = (data) => request.post('/scm/tender', data)
export const updateTender = (id, data) => request.put(`/scm/tender/${id}`, data)
export const deleteTender = (id) => request.delete(`/scm/tender/${id}`)
export const publishTender = (id) => request.post(`/scm/tender/${id}/publish`)
export const cancelTender = (id) => request.post(`/scm/tender/${id}/cancel`)
/** 代录线下标书：data { supplierId, deliveryDays, remark, items: [{ tenderItemId, price, taxRate }] } */
export const proxyBid = (id, data) => request.post(`/scm/tender/${id}/bid`, data)
export const openTender = (id) => request.post(`/scm/tender/${id}/open`)
/** 定标：data { supplierId, reason, target: CONTRACT | ORDER | NONE, warehouseId, startDate, endDate } */
export const awardTender = (id, data) => request.post(`/scm/tender/${id}/award`, data)

/** 送货通知（仓库收货） */
export const pageAsn = (params) => request.get('/scm/asn', { params })
export const getAsn = (id) => request.get(`/scm/asn/${id}`)
/** 收货：data { warehouseId, bizDate, lines: [{ asnItemId, qty, locationId }] }，返回采购入库单 ID */
export const receiveAsn = (id, data) => request.post(`/scm/asn/${id}/receive`, data)

/** 供应商门户账号 */
/** 门户账号：kind 为 supplier（供应商门户）或 customer（经销商门户） */
export const portalAccounts = (kind, partnerId) => request.get(`/scm/${kind}/${partnerId}/portal-accounts`)
export const addPortalAccount = (kind, partnerId, data) => request.post(`/scm/${kind}/${partnerId}/portal-accounts`, data)
export const changePortalAccountStatus = (kind, partnerId, userId, status) =>
  request.put(`/scm/${kind}/${partnerId}/portal-accounts/${userId}/status`, { status })
export const resetPortalAccountPassword = (kind, partnerId, userId, password) =>
  request.put(`/scm/${kind}/${partnerId}/portal-accounts/${userId}/password`, { password })
