import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

/** 请购单 */
export const pageRequisition = (params) => request.get('/scm/requisition', { params })
export const getRequisition = (id) => request.get(`/scm/requisition/${id}`)
export const addRequisition = (data) => request.post('/scm/requisition', data)
export const updateRequisition = (id, data) => request.put(`/scm/requisition/${id}`, data)
export const deleteRequisition = (id) => request.delete(`/scm/requisition/${ids(id)}`)
/** 返回 true 已发起审批流程，false 已直接批准 */
export const submitRequisition = (id) => request.post(`/scm/requisition/${id}/submit`)
/** 按供应商拆分生成草稿采购订单，返回订单 ID 列表 */
export const requisitionToOrder = (id, warehouseId) =>
  request.post(`/scm/requisition/${id}/to-order`, null, { params: { warehouseId } })
export const closeRequisition = (id) => request.post(`/scm/requisition/${id}/close`)

/** 询价比价 */
export const pageInquiry = (params) => request.get('/scm/inquiry', { params })
export const getInquiry = (id) => request.get(`/scm/inquiry/${id}`)
export const addInquiry = (data) => request.post('/scm/inquiry', data)
export const updateInquiry = (id, data) => request.put(`/scm/inquiry/${id}`, data)
export const deleteInquiry = (id) => request.delete(`/scm/inquiry/${ids(id)}`)
/** 录入供应商报价：{ supplierId, lines: [{ itemId, price, taxRate, leadDays, remark }] }，覆盖该供应商之前的报价 */
export const saveInquiryQuote = (id, data) => request.post(`/scm/inquiry/${id}/quote`, data)
export const deleteInquiryQuote = (id, supplierId) => request.delete(`/scm/inquiry/${id}/quote/${supplierId}`)
/** 定标完成：{ selections: { itemId: quoteId }, updatePrice, createOrder, warehouseId }，返回生成的采购订单 ID */
export const finishInquiry = (id, data) => request.post(`/scm/inquiry/${id}/finish`, data)
export const closeInquiry = (id) => request.post(`/scm/inquiry/${id}/close`)

/** 销售报价 */
export const pageQuotation = (params) => request.get('/scm/quotation', { params })
export const getQuotation = (id) => request.get(`/scm/quotation/${id}`)
export const addQuotation = (data) => request.post('/scm/quotation', data)
export const updateQuotation = (id, data) => request.put(`/scm/quotation/${id}`, data)
export const deleteQuotation = (id) => request.delete(`/scm/quotation/${ids(id)}`)
/** 返回 true 已发起审批流程，false 已直接批准 */
export const submitQuotation = (id) => request.post(`/scm/quotation/${id}/submit`)
/** 转销售订单，返回订单 ID */
export const quotationToOrder = (id, params) => request.post(`/scm/quotation/${id}/to-order`, null, { params })
export const loseQuotation = (id, reason) => request.post(`/scm/quotation/${id}/lose`, null, { params: { reason } })

/** 销售价目表 */
export const pagePriceList = (params) => request.get('/scm/price-list', { params })
export const addPriceList = (data) => request.post('/scm/price-list', data)
export const updatePriceList = (id, data) => request.put(`/scm/price-list/${id}`, data)
export const deletePriceList = (id) => request.delete(`/scm/price-list/${ids(id)}`)
/** 取价：{ materialId, customerId, qty, date }，返回 { price, taxRate, ... }，没有匹配的价格时为空 */
export const quotePrice = (params) => request.get('/scm/price-list/quote', { params })

/** 促销活动 */
export const pagePromotion = (params) => request.get('/scm/promotion', { params })
export const getPromotion = (id) => request.get(`/scm/promotion/${id}`)
export const promotionUsages = (id) => request.get(`/scm/promotion/${id}/usages`)
export const addPromotion = (data) => request.post('/scm/promotion', data)
export const updatePromotion = (id, data) => request.put(`/scm/promotion/${id}`, data)
export const changePromotionStatus = (id, status) => request.put(`/scm/promotion/${id}/status`, { status })
export const deletePromotion = (id) => request.delete(`/scm/promotion/${id}`)
/** 下单前预览：data { customerId, orderDate, items: [{ materialId, qty, price }], rebateAmount } → { lines, gifts, discountAmount, notes } */
export const previewPromotion = (data) => request.post('/scm/promotion/preview', data)

/** 销售返利 */
export const pageRebate = (params) => request.get('/scm/rebate', { params })
export const getRebate = (id) => request.get(`/scm/rebate/${id}`)
export const calcRebate = (id) => request.get(`/scm/rebate/${id}/calc`)
export const addRebate = (data) => request.post('/scm/rebate', data)
export const updateRebate = (id, data) => request.put(`/scm/rebate/${id}`, data)
export const deleteRebate = (id) => request.delete(`/scm/rebate/${id}`)
export const settleRebate = (id, settleDate) => request.post(`/scm/rebate/${id}/settle`, { settleDate })
export const unsettleRebate = (id) => request.post(`/scm/rebate/${id}/unsettle`)
/** 客户可用的返利余额 */
export const rebateBalance = (customerId, excludeOrderId) => request.get('/scm/rebate/balance', { params: { customerId, excludeOrderId } })
