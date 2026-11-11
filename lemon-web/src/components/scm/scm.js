import { t } from '@/locales'
import { money, round2, round4 } from '@/utils/number'
import { FreightBearer, FreightSettle } from '@/constants/enums'

/** 订单种类（与后端 OrderKind 的路径一致） */
export const ORDER_KINDS = {
  purchase: {
    label: t('scm.kinds.purchase'),
    partner: 'supplier',
    partnerLabel: t('scm.kinds.supplier'),
    warehouseLabel: t('scm.kinds.receiveWarehouse'),
    trades: ['purchaseIn', 'purchaseReturn']
  },
  sales: {
    label: t('scm.kinds.sales'),
    partner: 'customer',
    partnerLabel: t('scm.kinds.customer'),
    warehouseLabel: t('scm.kinds.shipWarehouse'),
    trades: ['salesOut', 'salesReturn']
  }
}

/**
 * 采购入库 / 退货、销售出库 / 退货（与后端 TradeKind 的路径一致）
 * - fromStock：明细从仓库现有库存中选择库位、批次、序列号（出库方向）
 * - returns：退货，数量上限为已完成数量；否则为未完成数量
 */
export const TRADE_KINDS = {
  purchaseIn: { label: t('scm.kinds.purchaseIn'), orderKind: 'purchase', fromStock: false, returns: false, action: t('scm.kinds.actionIn') },
  purchaseReturn: { label: t('scm.kinds.purchaseReturn'), orderKind: 'purchase', fromStock: true, returns: true, action: t('scm.kinds.actionReturn') },
  salesOut: { label: t('scm.kinds.salesOut'), orderKind: 'sales', fromStock: true, returns: false, action: t('scm.kinds.actionOut') },
  salesReturn: { label: t('scm.kinds.salesReturn'), orderKind: 'sales', fromStock: false, returns: true, action: t('scm.kinds.actionReturn') }
}

/**
 * 出入库单直接开单的空明细行（不含 key，由 useLineItems 生成）
 * @param taxRate 税率：表头发票类型的税率，没有时为业务参数「默认税率」（见 taxRate.js 的 useTaxRates().fallback）
 */
export const newTradeRow = (taxRate) => ({
  materialId: undefined, materialLabel: undefined, unit: undefined, batchEnabled: 0, serialEnabled: 0,
  qty: undefined, price: undefined, taxRate, priceAuto: true, priceSource: undefined,
  warehouseId: null, locationId: null, batchNo: undefined, slot: undefined, slotLabel: undefined, available: undefined, serialNos: undefined
})

/** 金额：千分位、2 位小数 */
export const formatMoney = (n) => money(n)

/** 价税合计 = 数量 × 含税单价（与后端一致，四舍五入到分） */
export const lineAmount = (qty, price) => Math.round((Number(qty) || 0) * (Number(price) || 0) * 100) / 100

/**
 * 优惠前的含税单价：整单优惠只在合计处扣减，明细显示原来的单价（价格体系不变）。
 * 出入库明细取后端记录的 grossPrice；订单明细按 (行金额 + 分摊的整单优惠) / 数量；没有分摊时即单价
 */
export const grossPrice = (i) => {
  if (i.grossPrice != null) return Number(i.grossPrice)
  return Number(i.discountShare) && Number(i.qty) ? round4((Number(i.amount) + Number(i.discountShare)) / Number(i.qty)) : Number(i.price)
}

/** 优惠前的金额 = 行金额 + 分摊的整单优惠（订单明细、出入库明细都适用） */
export const grossAmount = (i) => round2(Number(i.amount || 0) + Number(i.discountShare || 0))

/** 税额 = 价税合计 × 税率 / (100 + 税率) */
export const lineTax = (amount, rate) => (rate ? Math.round(((amount * rate) / (100 + Number(rate))) * 100) / 100 : 0)

/** 数量相减后保留 4 位小数，避免浮点误差（如 0.3 - 0.1）提交到后端超出精度 */
export const qty4 = round4

/** 订单明细的实际完成数量（可退数量） */
export const netQty = (i) => qty4(Number(i.doneQty || 0) - Number(i.returnQty || 0))

/** 订单明细的未出库 / 未入库数量（与后端一致：数量 − 已完成 + 退货补发；退款的退货不再补发） */
export const openQty = (i) => Math.max(qty4(Number(i.qty) - Number(i.doneQty || 0) + Number(i.replenishQty || 0)), 0)
export const remainingQty = openQty

/** 各种单据可选的运费承担（与后端 ScmTradeDocService.normalizeFreight 一致），第一个为默认值 */
export const FREIGHT_BEARERS = {
  purchaseIn: [FreightBearer.SUPPLIER, FreightBearer.COMPANY],
  salesOut: [FreightBearer.COMPANY, FreightBearer.CUSTOMER],
  salesReturn: [FreightBearer.CUSTOMER],
  purchaseReturn: [FreightBearer.SUPPLIER]
}

/** 销售出库、采购退货的运费要选择付给承运商的方式（现付 / 月结） */
export const freightSettled = (kind) => kind === 'salesOut' || kind === 'purchaseReturn'

/** 运费表单字段的初始值（新建、草稿回显） */
export const freightForm = (kind, d = {}) => ({
  freight: Number(d.freight) > 0 ? Number(d.freight) : undefined,
  freightBearer: d.freightBearer || FREIGHT_BEARERS[kind][0],
  freightSettle: d.freightSettle || FreightSettle.CASH,
  freightAccountId: d.freightAccountId || undefined,
  carrierSupplierId: d.carrierSupplierId || undefined,
  carrierSupplierName: d.carrierSupplierName || undefined
})

/** 运费的请求字段：没有运费时不传；采购入库只有本公司承担时传承运商，销售退货没有承运商和付款方式 */
export function freightPayload(kind, f) {
  if (!(Number(f.freight) > 0)) return {}
  if (kind === 'purchaseIn') {
    return { freight: f.freight, freightBearer: f.freightBearer, carrierSupplierId: f.freightBearer === FreightBearer.COMPANY ? f.carrierSupplierId : null }
  }
  if (!freightSettled(kind)) return { freight: f.freight, freightBearer: f.freightBearer }
  const cash = f.freightSettle === FreightSettle.CASH
  return {
    freight: f.freight,
    freightBearer: f.freightBearer,
    freightSettle: f.freightSettle,
    freightAccountId: cash ? f.freightAccountId : null,
    carrierSupplierId: f.carrierSupplierId || null
  }
}
