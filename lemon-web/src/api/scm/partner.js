import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

/** 供应商 / 客户：type = supplier | customer */
export const pagePartner = (type, params) => request.get(`/scm/${type}`, { params })
export const getPartner = (type, id) => request.get(`/scm/${type}/${id}`)
export const addPartner = (type, data) => request.post(`/scm/${type}`, data)
export const updatePartner = (type, id, data) => request.put(`/scm/${type}/${id}`, data)
export const changePartnerStatus = (type, id, status) => request.put(`/scm/${type}/${id}/status`, { status })
export const deletePartner = (type, id) => request.delete(`/scm/${type}/${ids(id)}`)
/** 启用的供应商 / 客户：[{ id, code, name, contact, phone, address }] */
export const partnerOptions = (type, keyword) => request.get(`/scm/${type}/options`, { params: { keyword } })

/** 供应商绩效 */
export const supplierKpi = (params) => request.get('/scm/supplier/kpi', { params })

/** 供货价格 */
export const pagePrice = (params) => request.get('/scm/supplier-price', { params })
export const supplierPrices = (supplierId) => request.get(`/scm/supplier-price/supplier/${supplierId}`)
export const addPrice = (data) => request.post('/scm/supplier-price', data)
export const updatePrice = (id, data) => request.put(`/scm/supplier-price/${id}`, data)
export const deletePrice = (id) => request.delete(`/scm/supplier-price/${ids(id)}`)

/** 供应商评估 */
export const pageEval = (params) => request.get('/scm/supplier-eval', { params })
export const addEval = (data) => request.post('/scm/supplier-eval', data)
export const deleteEval = (id) => request.delete(`/scm/supplier-eval/${ids(id)}`)
/** Excel 导入：{ rows: [{ rowNum, data }] } */
export const importPartner = (type, data) => request.post(`/scm/${type}/import`, data)
/** 客户信用占用：{ creditLimit, receivable, openOrders, available } */
export const customerCredit = (id) => request.get(`/scm/customer/${id}/credit`)
/** 供货价格的阶梯价：[{ minQty, price }] */
export const priceTiers = (id) => request.get(`/scm/supplier-price/${id}/tiers`)
/** 采购入库价格历史：{ materialId, supplierId } */
export const priceHistory = (params) => request.get('/scm/supplier-price/history', { params })
