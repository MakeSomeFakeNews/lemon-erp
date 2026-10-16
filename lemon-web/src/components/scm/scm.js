/** 订单种类（与后端 OrderKind 的路径一致） */
export const ORDER_KINDS = {
  purchase: {
    label: '采购订单',
    partner: 'supplier',
    partnerLabel: '供应商',
    warehouseLabel: '收货仓库',
    trades: ['purchaseIn', 'purchaseReturn']
  },
  sales: {
    label: '销售订单',
    partner: 'customer',
    partnerLabel: '客户',
    warehouseLabel: '发货仓库',
    trades: ['salesOut', 'salesReturn']
  }
}

/**
 * 采购入库 / 退货、销售出库 / 退货（与后端 TradeKind 的路径一致）
 * - fromStock：明细从仓库现有库存中选择库位、批次、序列号（出库方向）
 * - returns：退货，数量上限为已完成数量；否则为未完成数量
 */
export const TRADE_KINDS = {
  purchaseIn: { label: '采购入库', orderKind: 'purchase', fromStock: false, returns: false, action: '入库' },
  purchaseReturn: { label: '采购退货', orderKind: 'purchase', fromStock: true, returns: true, action: '退货' },
  salesOut: { label: '销售出库', orderKind: 'sales', fromStock: true, returns: false, action: '出库' },
  salesReturn: { label: '销售退货', orderKind: 'sales', fromStock: false, returns: true, action: '退货' }
}

/** 金额：千分位、2 位小数 */
export function formatMoney(n) {
  if (n === null || n === undefined || n === '') return '-'
  return Number(n).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 价税合计 = 数量 × 含税单价（与后端一致，四舍五入到分） */
export const lineAmount = (qty, price) => Math.round((Number(qty) || 0) * (Number(price) || 0) * 100) / 100

/** 税额 = 价税合计 × 税率 / (100 + 税率) */
export const lineTax = (amount, rate) => (rate ? Math.round(((amount * rate) / (100 + Number(rate))) * 100) / 100 : 0)

/** 数量相减后保留 4 位小数，避免浮点误差（如 0.3 - 0.1）提交到后端超出精度 */
export const qty4 = (n) => Math.round(n * 10000) / 10000

/** 订单明细的实际完成数量（可退数量） */
export const netQty = (i) => qty4(Number(i.doneQty || 0) - Number(i.returnQty || 0))

/** 订单明细的未出库 / 未入库数量（与后端一致：数量 − 已完成 + 退货补发；退款的退货不再补发） */
export const openQty = (i) => Math.max(qty4(Number(i.qty) - Number(i.doneQty || 0) + Number(i.replenishQty || 0)), 0)
export const remainingQty = openQty
