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
/** 经营历史：{ materialId, warehouseId?, beginDate?, endDate? } → { openingQty, openingAmount, rows, costVisible, ... } */
export const stockHistory = (params) => get('/wms/stock/history', params)
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
/** 采购入库草稿（业务参数 scm.tradeDoc.draft 开启且没有审核权限时，由有审核权限的人在 Web 端审核） */
export const addPurchaseInDraft = (data) => post('/scm/purchaseIn/drafts', data)

/** 发货通知（拣货） */
export const pageDelivery = (params) => get('/scm/delivery', params)
export const getDelivery = (id) => get(`/scm/delivery/${id}`)
export const pickDelivery = (id, data) => post(`/scm/delivery/${id}/pick`, data)

// ---------------------------------------------------------------- 快速开单（销售出库直接开单）

/** 客户选项 [{ id, code, name }]（登录即可访问） */
export const customerOptions = (keyword) => get('/scm/customer/options', { keyword })
/** 物料选项（登录即可访问） */
export const materialOptions = (keyword) => get('/wms/material/options', { keyword })
/** 物料的辅助单位 [{ unit, ratio, barcode }]，1 辅助单位 = ratio 基本单位（登录即可访问） */
export const materialUnits = (id) => get(`/wms/material/${id}/units`, undefined, { silent: true })
/**
 * 开单默认价（含税、基本单位）：{ kind: 'SALES', partnerId, materialIds: [id], qtys: [数量], date }
 * → 按行返回 [{ materialId, price, taxRate, source }]（与 materialIds 一一对应），与 Web 端 api/scm/pricing.js 的 defaultPrices 一致
 */
export const defaultPrices = (params) => get('/scm/pricing/default', params, { silent: true })
/** 价格参考：{ materialId, partnerId?, kind } → { sales, customerCompare, ... }，没有权限的分区不返回 */
export const priceReference = (params) => get('/scm/pricing/reference', params, { silent: true })
/**
 * 促销预览（scm:sales:add / scm:sales:edit / scm:salesOut:add）：{ customerId, orderDate, items: [{ materialId, qty, price }] }（只传普通行）
 * → { discountAmount, lines（与 items 一一对应）, gifts: [{ materialId, materialCode, materialName, spec, unit, batchEnabled, serialEnabled, qty, promotionId, promotionName }] }
 */
export const previewPromotion = (data) => post('/scm/promotion/preview', data, { silent: true })
/**
 * 销售出库（直接开单）：{ partnerId, salesmanId, warehouseId, bizDate, remark, wholeDiscount,
 *   items: [{ materialId, qty, price, taxRate, unitName, gift, warehouseId, locationId, batchNo, serialNos }] }
 * qty、price 为基本单位（price 为促销前原价），unitName 为录入单位（仅显示）；warehouseId 为明细仓库（scm.salesOut.lineWarehouse）；
 * 赠品行 gift: true（单价按 0，同一赠品可拆多行，合计须等于促销算出的数量；不带赠品行时后端从表头仓库先到期先出自动补上，序列号赠品除外）
 */
export const addSalesOut = (data) => post('/scm/salesOut', data)
/** 销售出库草稿（同上，请求与 addSalesOut 相同） */
export const addSalesOutDraft = (data) => post('/scm/salesOut/drafts', data)
