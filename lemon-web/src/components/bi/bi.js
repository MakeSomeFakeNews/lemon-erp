import { olapCubes } from '@/api/bi'
import { cached } from '@/utils/cache'
import { useDictStore } from '@/store/dict'
import { formatMoney } from '@/components/scm/scm'
import { formatQty } from '@/components/wms/wms'
import { findDict } from '@/utils/dict'
import { shortMoney } from '@/components/charts/charts'
import { t } from '@/locales'

/** 数据集元数据（缓存 1 分钟） */
export const loadCubes = () => cached('bi-cubes', olapCubes, 60000)

/** 相对日期范围 */
export const RANGES = [
  { value: 'THIS_MONTH', label: t('bi.range.thisMonth') },
  { value: 'THIS_QUARTER', label: t('bi.range.thisQuarter') },
  { value: 'THIS_YEAR', label: t('bi.range.thisYear') },
  { value: 'LAST_12M', label: t('bi.range.last12m') },
  { value: 'ALL', label: t('common.status.all') },
  { value: 'CUSTOM', label: t('bi.range.custom') }
]

export const CHART_TYPES = [
  { value: 'bar', label: t('bi.chart.bar') },
  { value: 'line', label: t('bi.chart.line') },
  { value: 'pie', label: t('bi.chart.pie') },
  { value: 'table', label: t('bi.chart.table') }
]

/** 钻取的默认下一级维度 */
const DRILL_NEXT = { year: 'quarter', quarter: 'period' }

/** 默认钻取目标：年 → 季度 → 月份；其他维度优先到物料，否则到月份 */
export function defaultDrill(cube, row) {
  const keys = cube.dims.map((d) => d.key)
  if (DRILL_NEXT[row] && keys.includes(DRILL_NEXT[row])) return DRILL_NEXT[row]
  if (row !== 'material' && keys.includes('material')) return 'material'
  if (row !== 'period' && keys.includes('period')) return 'period'
  return keys.find((k) => k !== row)
}

/** 按指标格式显示 */
export function formatMeasure(v, format) {
  if (v === null || v === undefined || v === '') return '-'
  const n = Number(v)
  switch (format) {
    case 'MONEY':
      return formatMoney(n)
    case 'QTY':
      return formatQty(n)
    case 'COUNT':
      return Math.round(n).toLocaleString('zh-CN')
    case 'PERCENT':
      return `${n.toFixed(2)}%`
    case 'PRICE':
      return n.toLocaleString('zh-CN', { maximumFractionDigits: 4 })
    default:
      return String(v)
  }
}

/** 维度值的显示名：ID 类维度取查询结果中的名称，字典维度取字典标签，月份格式化 */
export function dimLabelFn(cube, dimKey, labels = {}) {
  const store = useDictStore()
  const dim = cube?.dims.find((d) => d.key === dimKey)
  if (dim?.dictType) store.load(dim.dictType).catch(() => {})
  return (v) => {
    if (v === null || v === undefined) return t('bi.common.empty')
    if (v === '-') return t('common.field.total')
    if (labels[v]) return labels[v]
    if (dim?.dictType) return findDict(store.cache[dim.dictType], v)?.label || v
    if (dimKey === 'period' && /^\d{6}$/.test(v)) return `${v.slice(0, 4)}-${v.slice(4)}`
    if (dimKey === 'year') return t('bi.common.yearValue', { year: v })
    return v
  }
}

/** 实际显示的指标（配置为空时全部） */
export function activeMeasures(cube, config) {
  const keys = config?.measures?.length ? config.measures : cube.measures.map((m) => m.key)
  return cube.measures.filter((m) => keys.includes(m.key))
}

/** 图表展示的指标：选择了指标时取第一个，否则优先第一个金额指标 */
export function chartMeasure(cube, config) {
  const list = activeMeasures(cube, config)
  if (config?.measures?.length) return list[0]
  return list.find((m) => m.format === 'MONEY') || list[0]
}

/**
 * 透视结果转为图表：图表指标（chartMeasure），按行维度为横轴；有列维度时每个列值一个系列（饼图只取行合计）
 */
export function pivotChartOption(cube, config, result, type = 'bar') {
  const measure = chartMeasure(cube, config)
  if (!measure || !result) return null
  const rowLabel = dimLabelFn(cube, config.row, result.labels?.rows)
  const colLabel = dimLabelFn(cube, config.col, result.labels?.cols)
  const rows = result.rowTotals || []
  const categories = rows.map((r) => rowLabel(r.r))
  const val = (x) => (x === null || x === undefined ? null : Number(x))
  const percent = measure.format === 'PERCENT'
  const fmt = (v) => (percent ? `${Number(v).toFixed(1)}%` : measure.format === 'MONEY' ? shortMoney(v) : Number(v).toLocaleString('zh-CN'))
  if (type === 'pie') {
    return {
      tooltip: { trigger: 'item', valueFormatter: fmt },
      legend: { type: 'scroll', bottom: 0 },
      series: [{
        type: 'pie', radius: ['40%', '68%'], center: ['50%', '45%'], name: measure.label,
        data: rows.map((r, i) => ({ name: categories[i], value: val(r[measure.key]) }))
      }]
    }
  }
  let series
  if (config.col && result.colTotals?.length) {
    const index = new Map(result.cells.map((c) => [`${c.r}|${c.c}`, c]))
    series = result.colTotals.map((ct) => ({
      type, name: colLabel(ct.c), smooth: type === 'line',
      data: rows.map((r) => val(index.get(`${r.r}|${ct.c}`)?.[measure.key]))
    }))
  } else {
    series = [{ type, name: measure.label, smooth: type === 'line', data: rows.map((r) => val(r[measure.key])), barMaxWidth: 36 }]
  }
  return {
    tooltip: { trigger: 'axis', valueFormatter: fmt },
    legend: series.length > 1 ? { type: 'scroll', top: 0 } : undefined,
    grid: { left: 16, right: 16, top: series.length > 1 ? 36 : 16, bottom: 8, containLabel: true },
    xAxis: { type: 'category', data: categories, axisLabel: { interval: 'auto', hideOverlap: true } },
    yAxis: { type: 'value', axisLabel: { formatter: (v) => (percent ? `${v}%` : shortMoney(v)) } },
    series
  }
}
