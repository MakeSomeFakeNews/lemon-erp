import dayjs from 'dayjs'
import { t } from '@/locales'
import { JxcDimension, TradeKind } from '@/constants/enums'
import { findDict } from '@/utils/dict'
import { periodLabel } from '@/components/charts/charts'

const fmt = (d) => d.format('YYYY-MM-DD')

/** 默认日期范围：本月（月初到今天） */
export const thisMonth = () => [fmt(dayjs().startOf('month')), fmt(dayjs())]
export const thisYear = () => [fmt(dayjs().startOf('year')), fmt(dayjs())]
export const last12Months = () => [fmt(dayjs().subtract(11, 'month').startOf('month')), fmt(dayjs())]

/** 日期范围快捷选项（a-range-picker 的 presets） */
export function rangePresets() {
  return [
    { label: t('jxc.common.thisMonth'), value: [dayjs().startOf('month'), dayjs()] },
    { label: t('jxc.common.lastMonth'), value: [dayjs().subtract(1, 'month').startOf('month'), dayjs().subtract(1, 'month').endOf('month')] },
    { label: t('jxc.common.thisYear'), value: [dayjs().startOf('year'), dayjs()] },
    { label: t('jxc.common.last12Months'), value: [dayjs().subtract(11, 'month').startOf('month'), dayjs()] }
  ]
}

/** 汇总维度：销售报表、进货报表 */
export const SALES_DIMS = [JxcDimension.CATEGORY, JxcDimension.BRAND, JxcDimension.REGION, JxcDimension.CUSTOMER,
  JxcDimension.SALESMAN, JxcDimension.MATERIAL, JxcDimension.DAY, JxcDimension.MONTH]
export const PURCHASE_DIMS = [JxcDimension.CATEGORY, JxcDimension.BRAND, JxcDimension.SUPPLIER, JxcDimension.MATERIAL,
  JxcDimension.DAY, JxcDimension.MONTH]

/** 出入库单种类 → TradeDocDetail 的 kind */
export const TRADE_PATHS = {
  [TradeKind.SALES_OUT]: 'salesOut',
  [TradeKind.SALES_RETURN]: 'salesReturn',
  [TradeKind.PURCHASE_IN]: 'purchaseIn',
  [TradeKind.PURCHASE_RETURN]: 'purchaseReturn'
}

/** 物料分类、品牌（业务字典） */
export const DICT_TYPES = ['wms_material_category', 'wms_material_brand']

/** 汇总行的显示名称；dict 为 useDict(...DICT_TYPES) */
export function groupLabel(dim, row, dict) {
  const key = row.groupKey
  if (key === null || key === undefined || key === '') {
    return dim === JxcDimension.SALESMAN ? t('jxc.common.noSalesman') : t('jxc.common.unset')
  }
  switch (dim) {
    case JxcDimension.CATEGORY:
      return findDict(dict.wms_material_category, key)?.label || key
    case JxcDimension.BRAND:
      return findDict(dict.wms_material_brand, key)?.label || key
    case JxcDimension.MONTH:
      return periodLabel(key)
    case JxcDimension.DAY:
    case JxcDimension.REGION:
      return String(key)
    default:
      return row.groupName || String(key)
  }
}

export const dictLabel = (list, value) => (value ? findDict(list, value)?.label || value : '')

/** 毛利率文字 */
export const marginText = (v) => (v === null || v === undefined ? '-' : `${Number(v).toFixed(2)}%`)
