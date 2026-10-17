import request from '@/utils/request'

/** 采购 / 销售订单：kind = purchase | sales */
export const pageOrder = (kind, params) => request.get(`/scm/${kind}/order`, { params })
export const getOrder = (kind, id) => request.get(`/scm/${kind}/order/${id}`)
/** 返回新订单 ID */
export const addOrder = (kind, data) => request.post(`/scm/${kind}/order`, data)
export const updateOrder = (kind, id, data) => request.put(`/scm/${kind}/order/${id}`, data)
/** 返回 true 已发起审批流程，false 已直接审核 */
export const approveOrder = (kind, id) => request.post(`/scm/${kind}/order/${id}/approve`)
export const withdrawOrder = (kind, id) => request.post(`/scm/${kind}/order/${id}/withdraw`)
export const unapproveOrder = (kind, id) => request.post(`/scm/${kind}/order/${id}/unapprove`)
export const closeOrder = (kind, id) => request.post(`/scm/${kind}/order/${id}/close`)
/** 修改销售订单的业务员 */
export const changeSalesman = (id, salesmanId) => request.put(`/scm/sales/order/${id}/salesman`, { salesmanId })
export const deleteOrder = (kind, ids) => request.delete(`/scm/${kind}/order/${[].concat(ids).join(',')}`)
/** 单据追溯：订单的下游单据（按权限返回） */
export const orderTrace = (kind, id) => request.get(`/bi/trace/order/${kind}/${id}`)
/** 委外加工：委外订单的用料、发料、退料 */
export const outsourceMaterials = (orderId) => request.get(`/mes/outsource/${orderId}/materials`)
export const outsourceIssue = (orderId, data) => request.post(`/mes/outsource/${orderId}/issue`, data)
export const outsourceReturn = (orderId, data) => request.post(`/mes/outsource/${orderId}/return`, data)
/** 订单变更：data { reason, lines: [{ action: MODIFY | REMOVE | ADD, orderItemId, materialId, qty, price, taxRate, deliveryDate }] } */
export const changeOrder = (kind, id, data) => request.post(`/scm/${kind}/order/${id}/change`, data)
export const orderChanges = (kind, id) => request.get(`/scm/${kind}/order/${id}/changes`)
/** 寄售结算：客户寄售 → { orderId, tradeDocId }；供应商寄售 → { orderId, tradeDocId, consumeDocId } */
export const consignCustomer = (data) => request.post('/scm/consign/customer', data)
export const consignSupplier = (data) => request.post('/scm/consign/supplier', data)
