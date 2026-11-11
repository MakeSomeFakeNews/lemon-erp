import request from '@/utils/request'

/**
 * 商品速查（issue #97，需要 scm:quickLookup:list，与小程序共用）。价格为含税、基本单位、本位币，后端按权限过滤：
 * 进货类（参考进货价、最新进价）需要采购订单 / 采购入库查看权限，库存均价需要「查看成本金额」，售价类需要报价 / 销售订单 / 销售出库查看权限
 */

/** { keyword, price（QuickPrice）, current, size } → { rows: [{ materialId, code, name, spec, unit, imageUrl（主图）, qty, price }], price, priceLabel, priceDenied, hasMore } */
export const searchQuickLookup = (params) => request.get('/scm/quick-lookup', { params })
/** 「查询价格」可选项 [{ type, label, side, allowed }]（停用的价格列不列出） */
export const quickPriceOptions = () => request.get('/scm/quick-lookup/price-options')
/** 库存分布 [{ warehouseName, locationCode, batchNo, expiryDate, qty, frozenQty }] */
export const quickLookupStock = (materialId) => request.get(`/scm/quick-lookup/${materialId}/stock`)
/** 历史参考：kind SALES / PURCHASE → { recent, latest, max, min } */
export const quickLookupSummary = (materialId, kind) => request.get(`/scm/quick-lookup/${materialId}/summary`, { params: { kind } })
/** 往来明细：{ kind, keyword（单位编号 / 名称 / 电话）, start, end, current, size } → { records, total } */
export const quickLookupTrades = (materialId, params) => request.get(`/scm/quick-lookup/${materialId}/trades`, { params })

/** 商品图片（issue #94）：物料 ID 列表 → { 物料 ID: [URL] }（第一张为主图，公开文件地址），登录即可访问 */
export const materialImageUrls = (ids) => request.post('/wms/material/images/urls', ids)

/** 个人设置 quickLookup：{ price }（没有保存过时为 null） */
export const getQuickLookupSetting = () => request.get('/profile/settings/quickLookup')
export const saveQuickLookupSetting = (data) => request.put('/profile/settings/quickLookup', data)
