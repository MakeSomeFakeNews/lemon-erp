import { get, post, put } from '@/utils/request'

/** 仓库选项 [{ id, code, name }]（登录即可访问） */
export const warehouseOptions = () => get('/wms/warehouse/options')
/** 库位选项 [{ id, code, name }]（登录即可访问） */
export const locationOptions = (warehouseId) => get('/wms/location/options', { warehouseId })
/** 上架推荐：{ warehouseId, lines: [{ materialId, batchNo }] } → 与 lines 一一对应的 [{ locationId }] */
export const suggestPutaway = (data) => post('/wms/location/putaway', data)
/** 扫码：物料条码、包装条码或物料编码 → { material, unit, ratio }，找不到时为空 */
export const materialByBarcode = (code) => get('/wms/material/barcode', { code }, { silent: true })

/** 库存明细（仓库 + 库位 + 物料 + 批次）：{ current, size, warehouseId, locationId, materialId, keyword, batchNo } */
export const pageStock = (params) => get('/wms/stock', params)
/** 按物料汇总的库存：{ current, size, warehouseId, materialId, keyword, lowStock } */
export const pageStockSummary = (params) => get('/wms/stock/summary', params)
/** 在库序列号：{ warehouseId, locationId, materialId, batchNo } */
export const availableSerials = (params) => get('/wms/stock/serials', params)

/** 仓库单据：kind 为 in / out / transfer */
export const addDoc = (kind, data) => post(`/wms/${kind}`, data)
export const confirmDoc = (kind, id) => post(`/wms/${kind}/${id}/confirm`)

/** 盘点单 */
export const pageCheck = (params) => get('/wms/check', params)
export const getCheck = (id) => get(`/wms/check/${id}`)
export const updateCheck = (id, data) => put(`/wms/check/${id}`, data)

/** 采购订单（扫码收货） */
export const pagePurchaseOrder = (params) => get('/scm/purchase/order', params)
export const getPurchaseOrder = (id) => get(`/scm/purchase/order/${id}`)
/** 采购入库 */
export const addPurchaseIn = (data) => post('/scm/purchaseIn', data)

/** 发货通知（拣货） */
export const pageDelivery = (params) => get('/scm/delivery', params)
export const getDelivery = (id) => get(`/scm/delivery/${id}`)
export const pickDelivery = (id, data) => post(`/scm/delivery/${id}/pick`, data)
