import request from '@/utils/request'

/** 可承诺量：{ materialId, warehouseId, qty }，返回 { available, reserved, free, atp, promiseDate, lines } */
export const atp = (params) => request.get('/scm/atp', { params })

/** 欠交清单：{ kind: SALES | PURCHASE, overdueOnly, keyword, partnerId } */
export const pageBackorder = (params) => request.get('/scm/backorder', { params })

/** 销售订单库存预留 */
export const orderReservations = (orderId) => request.get(`/scm/sales/order/${orderId}/reservation`)
/** 手工预留：{ lines: [{ orderItemId, warehouseId, qty }] } */
export const reserveOrder = (orderId, data) => request.post(`/scm/sales/order/${orderId}/reservation`, data)
/** 一键预留：按空闲库存预留订单的未出库数量，返回预留的行数 */
export const autoReserveOrder = (orderId, warehouseId) =>
  request.post(`/scm/sales/order/${orderId}/reservation/auto`, null, { params: { warehouseId } })
/** 释放订单的全部预留 */
export const releaseOrderReservations = (orderId) => request.delete(`/scm/sales/order/${orderId}/reservation`)

/** 发货通知 */
export const pageDelivery = (params) => request.get('/scm/delivery', { params })
export const getDelivery = (id) => request.get(`/scm/delivery/${id}`)
/** 返回新发货通知 ID，保存后自动按先到期先出生成拣货建议 */
export const addDelivery = (data) => request.post('/scm/delivery', data)
/** 重新生成拣货建议（待拣货） */
export const suggestDelivery = (id) => request.post(`/scm/delivery/${id}/suggest`)
/** 确认拣货：{ picks: [{ deliveryItemId, locationId, batchNo, qty, serialNos }] } */
export const pickDelivery = (id, data) => request.post(`/scm/delivery/${id}/pick`, data)
/** 出库：生成销售出库单，返回出库单 ID */
export const shipDelivery = (id, bizDate) => request.post(`/scm/delivery/${id}/ship`, null, { params: { bizDate } })
export const cancelDelivery = (id) => request.post(`/scm/delivery/${id}/cancel`)
/** 装箱：[{ id, boxNo, seq, weight, volume, remark, items: [{ deliveryItemId, materialCode, materialName, unit, qty }] }] */
export const deliveryPackages = (id) => request.get(`/scm/delivery/${id}/packages`)
/** data：{ weight, volume, remark, items: [{ deliveryItemId, qty }] } */
export const addPackage = (id, data) => request.post(`/scm/delivery/${id}/packages`, data)
export const updatePackage = (id, packageId, data) => request.put(`/scm/delivery/${id}/packages/${packageId}`, data)
export const deletePackage = (id, packageId) => request.delete(`/scm/delivery/${id}/packages/${packageId}`)

/** 补货建议（再订货点）：{ keyword, onlyNeeded }，返回数组（不分页） */
export const listReorder = (params) => request.get('/scm/reorder', { params })
/** 选中的建议生成请购单（草稿）：{ items: [{ materialId, qty, supplierId, requiredDate }] }，返回请购单 ID */
export const reorderToRequisition = (data) => request.post('/scm/reorder/requisition', data)
