import { exportCustomerPrices } from '@/api/scm/pricing'
import { t } from '@/locales'
import { writeExcel } from '@/utils/excel'

/**
 * 客户执行价 Excel 导入导出的列（导出的文件修改后可直接导入）。
 * withCustomer：是否含客户编码列（客户列表的批量导入导出含；单个客户的「执行价」中不含，导入时取当前客户）
 */
export const CUSTOMER_PRICE_COLUMNS = (withCustomer) => [
  ...(withCustomer ? [{ title: t('pricing.customerPrice.customerCode'), key: 'customerCode', required: true, width: 16 }] : []),
  { title: t('pricing.customerPrice.materialCode'), key: 'materialCode', required: true, width: 16 },
  { title: t('pricing.customerPrice.price'), key: 'price', type: 'number', required: true, width: 14 },
  { title: t('pricing.customerPrice.taxRateCol'), key: 'taxRate', type: 'number', width: 12 },
  { title: t('pricing.customerPrice.remark'), key: 'remark', width: 24 }
]

export const priceImportSample = { customerCode: 'C0001', materialCode: 'M0001', price: 99.5, taxRate: 13, remark: '' }

/**
 * 导出执行价：params { customerId?, keyword? }；导出的列 = 导入列 + 客户名称、物料名称、规格、单位（只读参考，导入时忽略）
 */
export async function exportCustomerPriceRows(params, name) {
  const list = await exportCustomerPrices(params)
  const withCustomer = !params.customerId
  const cols = CUSTOMER_PRICE_COLUMNS(withCustomer)
  const extra = [
    ...(withCustomer ? [{ title: t('pricing.customerPrice.customerName'), key: 'customerName', width: 24 }] : []),
    { title: t('pricing.customerPrice.materialName'), key: 'materialName', width: 24 },
    { title: t('pricing.customerPrice.spec'), key: 'spec', width: 16 },
    { title: t('pricing.customerPrice.unit'), key: 'unit', width: 8 }
  ]
  const all = [...cols, ...extra]
  await writeExcel(
    name || t('pricing.customerPrice.exportName'),
    all.map((c) => ({ title: c.title, width: c.width })),
    list.map((r) => all.map((c) => (c.type === 'number' ? (r[c.key] == null ? '' : Number(r[c.key])) : r[c.key] ?? '')))
  )
}
