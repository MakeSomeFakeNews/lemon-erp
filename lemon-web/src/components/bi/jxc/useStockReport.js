import { reactive, ref } from 'vue'
import { t } from '@/locales'
import { writeExcel } from '@/utils/excel'
import { dictLabel } from '@/components/bi/jxc/jxc'
import { pageStock } from '@/api/wms/stock'

/** 导出：每次取的行数（分页上限）与最多导出的行数 */
const EXPORT_PAGE = 500
const EXPORT_MAX = 5000
/** 导出时按文字输出的列 */
const TEXT_KEYS = ['code', 'name', 'spec', 'unit', 'warehouseName', 'locationCode']

/** 去掉空的查询参数 */
export const cleanParams = (p) => Object.fromEntries(Object.entries(p).filter(([, v]) => v !== undefined && v !== null && v !== ''))

/** 联查库存分布（StockDistributionModal 的 loader） */
export const loadStock = async (materialId) => (await pageStock({ materialId, current: 1, size: 500 })).records

/**
 * 库存分析报表（周转、滞销 / 畅销、库存预警）的分页查询、合计与导出（issue #116，与积压分析相同的做法）。
 *
 * @param fetcher 接口函数（params → { records, total, summary, ... }）
 * @param params  返回本次查询参数（不含分页）的函数
 * @returns rows、summary、result（上一次查询的完整返回）、loading、exporting、pagination、load、search、onChange、exportRows
 */
export function useStockReport(fetcher, params) {
  const rows = ref([])
  const summary = ref(null)
  const result = ref({})
  const loading = ref(false)
  const exporting = ref(false)
  const pagination = reactive({ current: 1, pageSize: 20, total: 0, showSizeChanger: true, showTotal: (n) => t('common.table.total', { total: n }) })

  let seq = 0
  async function load() {
    const current = ++seq
    loading.value = true
    try {
      const res = await fetcher({ ...params(), current: pagination.current, size: pagination.pageSize })
      if (current !== seq) return
      rows.value = res.records
      pagination.total = res.total
      summary.value = res.summary
      result.value = res
    } finally {
      if (current === seq) loading.value = false
    }
  }

  function search() {
    pagination.current = 1
    load()
  }

  function onChange(p) {
    pagination.current = p.current
    pagination.pageSize = p.pageSize
    load()
  }

  /**
   * 导出全部页（最多 5000 行）和合计行
   *
   * @param name        文件名
   * @param cols        导出的列（页面的列，不含操作列）
   * @param dict        useDict 的结果（类别、品牌按名称导出）
   * @param summaryCell 合计行的单元格（列 → 文字或数字），为空时不导出合计行
   * @param cellText    个别列的导出文字（列, 行 → 值），返回 undefined 时按默认规则
   */
  async function exportRows(name, cols, dict, summaryCell, cellText) {
    exporting.value = true
    try {
      const all = []
      for (let page = 1; all.length < Math.min(pagination.total, EXPORT_MAX); page++) {
        const res = await fetcher({ ...params(), current: page, size: EXPORT_PAGE })
        all.push(...res.records)
        if (res.records.length < EXPORT_PAGE) break
      }
      const cell = (c, r) => {
        const custom = cellText?.(c, r)
        if (custom !== undefined) return custom
        if (c.key === 'category') return dictLabel(dict.wms_material_category, r.category)
        if (c.key === 'brand') return dictLabel(dict.wms_material_brand, r.brand)
        const v = r[c.key]
        if (c.date || TEXT_KEYS.includes(c.key)) return v || ''
        return v === null || v === undefined ? '' : Number(v)
      }
      const data = all.map((r) => cols.map((c) => cell(c, r)))
      if (summaryCell && summary.value) data.push(cols.map((c) => summaryCell(c, true)))
      await writeExcel(name, cols.map((c) => ({ title: c.title, width: Math.max(10, Math.round((c.width || 100) / 8)) })), data, { withStamp: false })
    } finally {
      exporting.value = false
    }
  }

  return { rows, summary, result, loading, exporting, pagination, load, search, onChange, exportRows }
}
