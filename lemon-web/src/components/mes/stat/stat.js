import { t } from '@/locales'
import { MesStatDimension as D, WoDueState } from '@/constants/enums'
import { periodLabel } from '@/components/charts/charts'
import { findDict } from '@/utils/dict'
import { money, qty } from '@/utils/number'

/**
 * 生产统计报表的配置（后端 /mes/stat，见 MesStatService）：每张报表的汇总维度、筛选条件、列、指标卡。
 * 列：{ key, title, width, align, type }，type 为 qty 数量 / money 金额 / rate 百分比 / int 计数 / date / text，
 * text 列可用 value(record, dict) 生成显示和导出的文字；status、dueState 列在页面中渲染为标签
 */

export const DICT_TYPES = ['wms_material_category', 'mes_wo_status']

const dim = (d) => t(`mesStat.dim.${d}`)
const col = (key, type, width, extra = {}) => ({
  key,
  title: t(`mesStat.field.${key}`),
  type,
  width,
  align: ['qty', 'money', 'rate', 'int'].includes(type) ? 'right' : undefined,
  ...extra
})
const text = (key, width, value, extra = {}) => col(key, 'text', width, { value, ...extra })

const operatorCol = () => text('operator', 130, (r) => r.operatorName || t('mesStat.common.unset'), { fixed: 'left' })
const deptCol = () => text('dept', 120, (r) => r.deptName || '')
const workCenterCol = () => text('workCenter', 140, (r) => r.workCenterName || t('mesStat.common.noWorkCenter'))
const opCol = () => text('op', 130, (r) => r.opName || '')
const productCol = (fixed = true) => text('product', 220, (r) => `${r.productCode || ''} ${r.productName || ''}`.trim(), fixed ? { fixed: 'left' } : {})
const dayCol = () => text('day', 110, (r) => r.day || '', { fixed: 'left' })
const monthCol = () => text('month', 100, (r) => periodLabel(r.period), { fixed: 'left' })
const deptGroupCol = () => text('workshop', 140, (r) => r.deptName || t('mesStat.common.unset'), { fixed: 'left' })
const categoryCol = (fixed = true) => text('category', 120, (r, dict) => (r.category ? findDict(dict.wms_material_category, r.category)?.label || r.category : t('mesStat.common.unset')), fixed ? { fixed: 'left' } : {})

/** 报工汇总的指标列 */
const reportMeasures = (piece) => [
  col('goodQty', 'qty', 110),
  col('scrapQty', 'qty', 100),
  col('yieldRate', 'rate', 90),
  col('hours', 'qty', 100),
  col('stdHours', 'qty', 110),
  col('efficiency', 'rate', 90),
  ...(piece ? [col('pieceWage', 'money', 120)] : [col('operatorCount', 'int', 80)]),
  col('reportCount', 'int', 90),
  col('woCount', 'int', 80)
]

export const REPORTS = {
  piece: {
    title: 'mesStat.piece.title',
    tip: 'mesStat.piece.tip',
    dims: [D.OPERATOR, D.OPERATOR_OP, D.OPERATOR_DAY],
    filters: ['operator', 'workCenter', 'keyword'],
    drill: true,
    columns: (d) => [
      operatorCol(),
      ...(d === D.OPERATOR ? [deptCol()] : []),
      ...(d === D.OPERATOR_OP ? [workCenterCol(), opCol()] : []),
      ...(d === D.OPERATOR_DAY ? [text('day', 110, (r) => r.day || '')] : []),
      ...reportMeasures(true)
    ],
    kpis: [
      { key: 'operatorCount', type: 'int' },
      { key: 'goodQty', type: 'qty' },
      { key: 'scrapQty', type: 'qty' },
      { key: 'yieldRate', type: 'rate' },
      { key: 'hours', type: 'qty' },
      { key: 'pieceWage', type: 'money' }
    ],
    /** 汇总行穿透到报工明细的筛选条件 */
    drillParams: (d, r) => ({
      operatorId: r.operatorId ?? undefined,
      ...(d === D.OPERATOR_OP ? workCenterParams(r) : {}),
      ...(d === D.OPERATOR_OP ? { opName: r.opName } : {}),
      ...(d === D.OPERATOR_DAY ? { day: r.day } : {})
    })
  },
  output: {
    title: 'mesStat.output.title',
    tip: 'mesStat.output.tip',
    dims: [D.WORK_CENTER, D.OPERATION, D.PRODUCT, D.DAY, D.MONTH],
    filters: ['workCenter', 'operator', 'keyword'],
    drill: true,
    columns: (d) => [
      ...(d === D.WORK_CENTER ? [{ ...workCenterCol(), fixed: 'left' }] : []),
      ...(d === D.OPERATION ? [{ ...workCenterCol(), fixed: 'left' }, opCol()] : []),
      ...(d === D.PRODUCT ? [productCol(), text('unit', 70, (r) => r.unit || '')] : []),
      ...(d === D.DAY ? [dayCol()] : []),
      ...(d === D.MONTH ? [monthCol()] : []),
      ...reportMeasures(false)
    ],
    kpis: [
      { key: 'goodQty', type: 'qty' },
      { key: 'scrapQty', type: 'qty' },
      { key: 'yieldRate', type: 'rate' },
      { key: 'hours', type: 'qty' },
      { key: 'stdHours', type: 'qty' },
      { key: 'efficiency', type: 'rate' }
    ],
    drillParams: (d, r) => ({
      ...([D.WORK_CENTER, D.OPERATION].includes(d) ? workCenterParams(r) : {}),
      ...(d === D.OPERATION ? { opName: r.opName } : {}),
      ...(d === D.PRODUCT ? { productId: r.productId } : {}),
      ...(d === D.DAY ? { day: r.day } : {}),
      ...(d === D.MONTH ? monthRange(r.period) : {})
    })
  },
  wo: {
    title: 'mesStat.wo.title',
    tip: 'mesStat.wo.tip',
    dims: [D.WORK_ORDER, D.PRODUCT, D.DEPT, D.MONTH],
    filters: ['status', 'keyword'],
    columns: (d) =>
      d === D.WORK_ORDER
        ? [
            text('woNo', 150, (r) => r.woNo, { fixed: 'left' }),
            productCol(false),
            col('status', 'text', 90),
            text('planEnd', 110, (r) => r.planEnd || ''),
            text('finishTime', 110, (r) => (r.finishTime || r.closeTime || '').slice(0, 10)),
            col('dueState', 'text', 110),
            col('delayDays', 'int', 90),
            col('qty', 'qty', 100),
            col('finishedQty', 'qty', 100),
            col('scrapQty', 'qty', 90),
            col('completionRate', 'rate', 90),
            col('yieldRate', 'rate', 90),
            text('workshop', 120, (r) => r.deptName || '')
          ]
        : [
            ...(d === D.PRODUCT ? [productCol(), text('unit', 70, (r) => r.unit || '')] : []),
            ...(d === D.DEPT ? [deptGroupCol()] : []),
            ...(d === D.MONTH ? [monthCol()] : []),
            col('woCount', 'int', 80),
            col('qty', 'qty', 110),
            col('finishedQty', 'qty', 110),
            col('scrapQty', 'qty', 100),
            col('completionRate', 'rate', 90),
            col('yieldRate', 'rate', 90),
            col('completedCount', 'int', 90),
            col('onTimeCount', 'int', 90),
            col('lateCount', 'int', 90),
            col('overdueCount', 'int', 100),
            col('closedCount', 'int', 80),
            col('onTimeRate', 'rate', 100)
          ],
    kpis: [
      { key: 'woCount', type: 'int' },
      { key: 'completionRate', type: 'rate' },
      { key: 'yieldRate', type: 'rate' },
      { key: 'onTimeRate', type: 'rate' },
      { key: 'lateCount', type: 'int' },
      { key: 'overdueCount', type: 'int', danger: true }
    ]
  },
  product: {
    title: 'mesStat.product.title',
    tip: 'mesStat.product.tip',
    dims: [D.PRODUCT, D.CATEGORY, D.DEPT, D.DAY, D.MONTH],
    filters: ['category', 'keyword'],
    columns: (d, costVisible) => [
      ...(d === D.PRODUCT
        ? [productCol(), text('spec', 120, (r) => r.spec || ''), text('unit', 70, (r) => r.unit || ''), categoryCol(false)]
        : []),
      ...(d === D.CATEGORY ? [categoryCol()] : []),
      ...(d === D.DEPT ? [deptGroupCol()] : []),
      ...(d === D.DAY ? [dayCol()] : []),
      ...(d === D.MONTH ? [monthCol()] : []),
      col('finishQty', 'qty', 120),
      col('scrapInQty', 'qty', 120),
      col('finishCount', 'int', 100),
      col('woCount', 'int', 80),
      ...(costVisible ? [col('finishCost', 'money', 130), ...(d === D.PRODUCT ? [col('unitCost', 'price', 110, { align: 'right' })] : [])] : [])
    ],
    kpis: [
      { key: 'finishQty', type: 'qty' },
      { key: 'scrapInQty', type: 'qty' },
      { key: 'finishCount', type: 'int' },
      { key: 'woCount', type: 'int' },
      { key: 'finishCost', type: 'money', cost: true }
    ]
  },
  usage: {
    title: 'mesStat.usage.title',
    tip: 'mesStat.usage.tip',
    dims: [D.MATERIAL, D.WO_MATERIAL],
    filters: ['keyword'],
    noTotal: true,
    columns: (d) => [
      ...(d === D.WO_MATERIAL
        ? [
            text('woNo', 150, (r) => r.woNo, { fixed: 'left' }),
            col('status', 'text', 90),
            productCol(false),
            text('finishedOfQty', 130, (r) => `${qty(r.finishedQty)} / ${qty(r.woQty)}`)
          ]
        : []),
      text('material', 220, (r) => `${r.materialCode || ''} ${r.materialName || ''}`.trim(), d === D.MATERIAL ? { fixed: 'left' } : {}),
      text('spec', 120, (r) => r.spec || ''),
      text('unit', 70, (r) => r.unit || ''),
      ...(d === D.MATERIAL ? [col('woCount', 'int', 80)] : []),
      col('stdQty', 'qty', 120),
      col('actualQty', 'qty', 120),
      col('varianceQty', 'qty', 120),
      col('varianceRate', 'rate', 100)
    ],
    kpis: []
  }
}

/** 工作中心穿透条件：工作中心为空的行按「未设置工作中心」筛选 */
function workCenterParams(r) {
  return r.workCenterId == null ? { noWorkCenter: true } : { workCenterId: r.workCenterId }
}

/** 月份 yyyyMM 的首尾日期（穿透时代替期间） */
function monthRange(period) {
  const y = Number(String(period).slice(0, 4))
  const m = Number(String(period).slice(4))
  const last = new Date(y, m, 0).getDate()
  const mm = String(m).padStart(2, '0')
  return { begin: `${y}-${mm}-01`, end: `${y}-${mm}-${String(last).padStart(2, '0')}` }
}

export const dimLabel = dim

/** 交期状态的标签颜色 */
export const DUE_COLORS = {
  [WoDueState.ON_TIME]: 'green',
  [WoDueState.LATE]: 'orange',
  [WoDueState.OVERDUE]: 'red',
  [WoDueState.OPEN]: 'blue',
  [WoDueState.CLOSED]: 'default'
}

/** 百分比文字 */
export const rateText = (v) => (v === null || v === undefined ? '-' : `${Number(v).toFixed(1)}%`)

/** 单元格显示的文字（status、dueState 以外） */
export function cellText(c, r, dict) {
  const v = r[c.key]
  switch (c.type) {
    case 'text':
      return c.value ? c.value(r, dict) : v ?? ''
    case 'qty':
      return qty(v)
    case 'money':
      return money(v)
    case 'price':
      return v === null || v === undefined ? '-' : money(v)
    case 'rate':
      return rateText(v)
    default:
      return v ?? ''
  }
}

/** 导出的单元格：数量、金额、比率导出数值，状态导出名称 */
export function exportCell(c, r, dict) {
  if (c.key === 'status') return findDict(dict.mes_wo_status, r.status)?.label || r.status || ''
  if (c.key === 'dueState') return r.dueState ? t(`mesStat.due.${r.dueState}`) : ''
  if (c.type === 'text') return cellText(c, r, dict)
  const v = r[c.key]
  return v === null || v === undefined ? '' : Number(v)
}
