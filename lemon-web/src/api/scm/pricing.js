import request from '@/utils/request'

const list = (v) => (Array.isArray(v) ? v.join(',') : v)

/**
 * 开单默认价（含税、基本单位、本位币）
 * params: { kind: 'SALES' | 'PURCHASE', partnerId, materialIds: [id] | '1,2', qtys?: [数量]（与 materialIds 一一对应）, date? }
 * → [{ materialId, price, taxRate, source }]，source 见 @/constants/enums 的 PriceSource（没有价格时 price、source 为空，
 * taxRate 为空表示来源没有税率，保持原税率）。
 * 优先级：销售 客户执行价 → 价目表 → 客户价格等级价 → 零售价 → 最近售价；采购 合同价 → 供货价 → 最近进价 → 参考成本价
 */
export const defaultPrices = (params) =>
  request.get('/scm/pricing/default', {
    params: {
      ...params,
      kind: params.kind ? String(params.kind).toUpperCase() : params.kind,
      materialIds: list(params.materialIds),
      qtys: params.qtys ? list(params.qtys.map((q) => (q == null || q === '' ? 1 : q))) : undefined
    }
  })

/**
 * 价格参考：params { materialId, partnerId?, kind?（partnerId 是客户 SALES 还是供应商 PURCHASE，指定时只筛选该方向的分区）,
 *   keyword?（往来单位编码 / 名称）, start?, end? }
 * → { quotations, sales, purchases, purchaseQty, supplierCompare, customerCompare }，没有权限的分区不返回
 */
export const priceReference = (params) => request.get('/scm/pricing/reference', { params })

/** 商品价格批量维护 */
export const pageGoodsPrice = (params) => request.get('/scm/goods-price', { params })
/** [{ id, retailPrice, wholesalePrice1..8 }] → 更新的物料数 */
export const saveGoodsPrices = (rows) => request.put('/scm/goods-price', rows)

/** 客户执行价：[{ materialId, price, taxRate, remark, materialCode, materialName, spec, unit }] */
export const customerPrices = (customerId) => request.get(`/scm/customer/${customerId}/prices`)
export const saveCustomerPrices = (customerId, rows) => request.put(`/scm/customer/${customerId}/prices`, rows)

/** 价格列设置：[{ level（0 零售价，1-8 批发价）, name, status }]，登录即可读取 */
export const priceLevels = () => request.get('/wms/price-level')
export const savePriceLevels = (rows) => request.put('/wms/price-level', rows)
