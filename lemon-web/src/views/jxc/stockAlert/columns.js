import { t } from '@/locales'
import { StockAlertTab } from '@/constants/enums'
import { actionColumn } from '@/components/common/tableActions'
import { goodsColumns } from '@/components/bi/jxc/useStockReport'

const q = (key, label, extra = {}) => ({ title: t(label), key, width: 104, align: 'right', qty: true, sortable: true, ...extra })
const m = (key, label, extra = {}) => ({ title: t(label), key, width: 116, align: 'right', money: true, sortable: true, ...extra })
const d = (key, label, tip) => ({ title: t(label), key, width: 108, date: true, sortable: true, tip: t(tip) })

const goods = () => goodsColumns()

/**
 * 库存预警各标签页的列（issue #116），sortable 的列可点击列头排序（issue #122，服务端排序，后端按标签页和权限校验）
 *
 * @param tab        StockAlertTab
 * @param result     上一次查询的返回（costVisible、purchaseVisible、salesVisible）
 * @param levels     启用的价格列 [{ level, name, field }]（usePriceLevels 的 enabledLevels）
 * @param withAction 是否有操作列（联查经营历史、库存分布）
 */
export function alertColumns(tab, result, levels, withAction) {
  const cost = !!result.costVisible
  const action = withAction ? [actionColumn(2)] : []
  const warehouse = { title: t('jxc.stockAlert.warehouse'), key: 'warehouseName', width: 130, ellipsis: true, sortable: true }
  if (tab === StockAlertTab.WAREHOUSE) {
    return [
      d('createDate', 'jxc.stockAlert.createDate', 'jxc.stockAlert.createDateTip'),
      d('lastInDate', 'jxc.stockAlert.lastIn', 'jxc.stockAlert.lastInTip'),
      d('lastSaleDate', 'jxc.stockAlert.lastSale', 'jxc.stockAlert.lastSaleTip'),
      warehouse,
      ...goods(),
      q('monthQty', 'jxc.stockAlert.monthQty', { tip: t('jxc.stockAlert.monthQtyTip') }),
      q('orderQty', 'jxc.stockAlert.orderQty', { tip: t('jxc.stockAlert.orderQtyTip') }),
      q('stockQty', 'jxc.stockAlert.stockQty'),
      ...(cost ? [m('stockAmount', 'jxc.stockAlert.stockAmount', { tip: t('jxc.stockAlert.stockAmountTip') }),
        m('unitCost', 'jxc.stockAlert.unitCost', { tip: t('jxc.stockAlert.unitCostTip'), price: true })] : []),
      ...(result.purchaseVisible ? [m('refCost', 'jxc.stockAlert.refCost', { tip: t('jxc.stockAlert.refCostTip'), price: true })] : []),
      ...(result.salesVisible ? levels.map((l) => ({ title: l.name, key: l.field, width: 104, align: 'right', money: true, price: true, sortable: true })) : []),
      q('maxQty', 'jxc.stockAlert.maxQty', { tip: t('jxc.stockAlert.limitTip') }),
      q('minQty', 'jxc.stockAlert.minQty', { tip: t('jxc.stockAlert.limitTip') }),
      { title: t('jxc.stockAlert.location'), dataIndex: 'locationCode', key: 'locationCode', width: 100, sortable: true, tip: t('jxc.stockAlert.locationTip') },
      ...action
    ]
  }
  const p = tab === StockAlertTab.HIGH ? 'high' : 'low'
  return [
    ...goods(),
    warehouse,
    q('monthQty', 'jxc.stockAlert.monthQty', { tip: t('jxc.stockAlert.monthQtyTip') }),
    q('stockQty', 'jxc.stockAlert.stockQty'),
    ...(cost ? [m('stockAmount', 'jxc.stockAlert.stockFund', { tip: t('jxc.stockAlert.stockAmountTip') })] : []),
    q('baseQty', `jxc.stockAlert.${p}Base`, { tip: t(`jxc.stockAlert.${p}BaseTip`) }),
    { title: t(`jxc.stockAlert.${p}Ratio`), key: 'ratio', width: 104, align: 'right', sortable: true, tip: t(`jxc.stockAlert.${p}RatioTip`) },
    q('gapQty', `jxc.stockAlert.${p}Qty`, { tip: t(`jxc.stockAlert.${p}QtyTip`) }),
    ...(cost ? [m('gapAmount', `jxc.stockAlert.${p}Amount`, { tip: t(`jxc.stockAlert.${p}AmountTip`) })] : []),
    ...action
  ]
}

/** 合计行有值的列 */
export const SUMMARY_KEYS = ['monthQty', 'orderQty', 'stockQty', 'stockAmount', 'baseQty', 'gapQty', 'gapAmount']
