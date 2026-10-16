import request from '@/utils/request'

/** 供应商门户：供应商一律取当前账号关联的供应商 */
export const pagePortalOrder = (params) => request.get('/portal/order', { params })
export const getPortalOrder = (id) => request.get(`/portal/order/${id}`)
/** data { accept, promisedDate, remark } */
export const confirmPortalOrder = (id, data) => request.post(`/portal/order/${id}/confirm`, data)
export const orderOpenLines = (id) => request.get(`/portal/order/${id}/open-lines`)

export const pagePortalAsn = (params) => request.get('/portal/asn', { params })
export const getPortalAsn = (id) => request.get(`/portal/asn/${id}`)
export const addPortalAsn = (data) => request.post('/portal/asn', data)
export const cancelPortalAsn = (id) => request.post(`/portal/asn/${id}/cancel`)

export const pagePortalStatement = (params) => request.get('/portal/statement', { params })
export const getPortalStatement = (id) => request.get(`/portal/statement/${id}`)
/** data { agree, remark, invoiceNo, invoiceAmount, invoiceDate } */
export const confirmPortalStatement = (id, data) => request.post(`/portal/statement/${id}/confirm`, data)
export const savePortalInvoice = (id, data) => request.put(`/portal/statement/${id}/invoice`, data)

export const pagePortalTender = (params) => request.get('/portal/tender', { params })
export const getPortalTender = (id) => request.get(`/portal/tender/${id}`)
/** data { deliveryDays, remark, items: [{ tenderItemId, price, taxRate }] } */
export const bidPortalTender = (id, data) => request.post(`/portal/tender/${id}/bid`, data)

export const pagePortalClaim = (params) => request.get('/portal/claim', { params })
