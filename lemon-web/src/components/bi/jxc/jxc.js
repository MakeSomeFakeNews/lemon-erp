import dayjs from 'dayjs'
import { FileTextOutlined, UnorderedListOutlined } from '@ant-design/icons-vue'
import { t } from '@/locales'
import { JxcDimension, TradeKind } from '@/constants/enums'
import { findDict } from '@/utils/dict'
import { periodLabel } from '@/components/charts/charts'
import { actionColumn } from '@/components/common/tableActions'

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

/** 物料分类、品牌、客户区域（业务字典） */
export const DICT_TYPES = ['wms_material_category', 'wms_material_brand', 'scm_region']

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
    case JxcDimension.REGION:
      return findDict(dict.scm_region, key)?.label || key
    case JxcDimension.DAY:
      return String(key)
    default:
      return row.groupName || String(key)
  }
}

export const dictLabel = (list, value) => (value ? findDict(list, value)?.label || value : '')

/** 毛利率文字 */
export const marginText = (v) => (v === null || v === undefined ? '-' : `${Number(v).toFixed(2)}%`)

/**
 * 汇总行的两种穿透（TableActions 的 actions）：【单据】汇总 → 单据列表（JxcDocs）→ 单据明细；【商品明细】汇总 → 出入库明细（JxcDetail）
 */
export function drillActions(onDocs, onLines) {
  return [
    { key: 'docs', label: t('jxc.common.drillDocs'), icon: FileTextOutlined, onClick: onDocs },
    { key: 'lines', label: t('jxc.common.drillLines'), icon: UnorderedListOutlined, onClick: onLines }
  ]
}

/** 与商品直接相关的汇总维度：点击汇总行默认到商品明细；客户、供货商、区域、业务员、日、月等默认到单据列表 */
const LINE_DIMS = [JxcDimension.CATEGORY, JxcDimension.BRAND, JxcDimension.MATERIAL]
export const drillsToLines = (dim) => LINE_DIMS.includes(dim)

/** 穿透操作列（两个图标） */
export const drillColumn = (extra = {}) => actionColumn(2, [], extra)
