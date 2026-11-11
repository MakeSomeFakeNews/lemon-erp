import { get, post, put } from '@/utils/request'

/**
 * 商品速查（issue #97，需要 scm:quickLookup:list）。价格均为含税、基本单位、本位币，后端按权限过滤：
 * 进货类（参考进货价、最新进价）需要采购订单 / 采购入库查看权限，库存均价需要「查看成本金额」，售价类需要报价 / 销售订单 / 销售出库查看权限
 */

/** 每页条数 */
export const LOOKUP_PAGE_SIZE = 20

/**
 * 搜索商品：{ keyword, price（QuickPrice）, current } → { rows: [{ materialId, code, name, spec, unit, imageUrl, qty, price }], price, priceLabel, priceDenied, hasMore }
 * 关键字规则同物料下拉（编码、名称、规格、条码、拼音首字母、去掉分隔符的缩写）；price 没有权限时不返回价格（priceDenied 为 true）
 */
export const searchLookup = (params) => get('/scm/quick-lookup', { size: LOOKUP_PAGE_SIZE, ...params })
/** 「查询价格」可选项 [{ type, label, side（PURCHASE / COST / SALES）, allowed }]，停用的价格列不列出 */
export const lookupPriceOptions = () => get('/scm/quick-lookup/price-options')
/** 库存分布：[{ warehouseName, locationCode, batchNo, expiryDate, qty, frozenQty }] */
export const lookupStock = (materialId) => get(`/scm/quick-lookup/${materialId}/stock`)
/** 历史参考：kind SALES / PURCHASE → { recent（最近 3 次）, latest, max, min }，每项 { bizDate, partnerName, qty, price, docNo } */
export const lookupSummary = (materialId, kind, partnerId) => get(`/scm/quick-lookup/${materialId}/summary`, { kind, partnerId }, { silent: true })
/** 往来明细：{ kind, keyword（单位编码 / 名称 / 电话）, start, end, current, size } → { records, total } */
export const lookupTrades = (materialId, params) => get(`/scm/quick-lookup/${materialId}/trades`, params)
/** 价格参考（与 Web 端销售单明细的「价格参考」相同）：{ materialId, keyword?, start?, end? } → 各分区，没有权限的分区不返回 */
export const lookupReference = (params) => get('/scm/pricing/reference', params, { silent: true })
/** 商品图片（issue #94）：物料 ID 列表 → { 物料 ID: [URL] }（第一张为主图，公开文件地址，用 fileUrl 转为完整地址），登录即可访问 */
export const materialImageUrls = (ids) => post('/wms/material/images/urls', ids, { silent: true })

/** 个人设置 quickLookup：{ price } */
export const SETTING_KEY = 'quickLookup'
export const getLookupSetting = () => get(`/profile/settings/${SETTING_KEY}`, undefined, { silent: true })
export const saveLookupSetting = (value) => put(`/profile/settings/${SETTING_KEY}`, value)
