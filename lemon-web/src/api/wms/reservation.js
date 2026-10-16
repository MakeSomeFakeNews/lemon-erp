import request from '@/utils/request'

/** 库存预留：{ warehouseId, keyword, sourceNo } */
export const pageReservation = (params) => request.get('/wms/reservation', { params })
/** 释放预留 */
export const releaseReservation = (id) => request.delete(`/wms/reservation/${id}`)
/** 空闲库存（可用 − 已预留）：返回 { 物料ID: 数量 } */
export const freeStock = (warehouseId, materialIds) =>
  request.get('/wms/reservation/free', { params: { warehouseId, materialIds: [].concat(materialIds).join(',') } })
