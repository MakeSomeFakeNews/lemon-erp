import request from '@/utils/request'

/** 库存明细（仓库 + 库位 + 物料 + 批次） */
export const pageStock = (params) => request.get('/wms/stock', { params })
/** 按物料汇总 */
export const pageStockSummary = (params) => request.get('/wms/stock/summary', { params })
/** 某个库存位置上的在库序列号：{ warehouseId, locationId?, materialId, batchNo? } */
export const availableSerials = (params) => request.get('/wms/stock/serials', { params })

export const pageInOut = (params) => request.get('/wms/stock/in-out', { params })
export const pageFlow = (params) => request.get('/wms/flow', { params })

export const pageBatch = (params) => request.get('/wms/trace/batch', { params })
export const getBatch = (id) => request.get(`/wms/trace/batch/${id}`)
export const pageSerial = (params) => request.get('/wms/trace/serial', { params })
export const serialLogs = (id) => request.get(`/wms/trace/serial/${id}/logs`)
