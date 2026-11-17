import { reactive } from 'vue'
import { currentLocale } from '@/locales'

/**
 * 表格列头排序（issue #122）：列定义写 sortable: true，点击列头 升序 → 降序 → 取消（Ant Design Vue 的 sorter / sortOrder，
 * 列头提示「点击升序 / 点击降序 / 取消排序」随界面语言）。合计行（#summary）不参与排序。
 *
 * - remote = true：服务端排序（分页报表，排序作用于全部数据）。查询参数为 sortField（列的 sortField，默认为 key / dataIndex，
 *   与后端白名单中的字段名一致）、sortOrder（asc | desc），由 params() 给出；后端拒绝不在白名单中的字段。
 * - remote = false：前端排序（一次取全部数据的报表）。右对齐的列（数量、金额）按数值，其他按文本（localeCompare，数字部分按数值，
 *   ISO 日期即时间顺序）；列可写 sortValue: (record) => 值 指定排序值（如显示名称），sortType: 'number' | 'text' 指定比较方式。
 *   空值始终排在最后。导出时用 sortRows(rows, columns) 得到与页面一致的顺序。
 *
 * 用法：const sorter = useTableSort({ remote: true })；列 computed(() => sorter.columns([...]))；
 * a-table 的 @change="(p, f, s) => { if (sorter.change(s)) 回到第一页并重新查询 }"
 */
export function useTableSort({ remote = false } = {}) {
  const sort = reactive({ field: null, order: null })

  /** 给 sortable 的列加上 sorter（受控的 sortOrder，切换数据后状态不丢） */
  function columns(cols) {
    return cols.map((c) => {
      if (!c.sortable) return c
      const field = sortFieldOf(c)
      return {
        ...c,
        width: Number(c.width) ? Number(c.width) + SORTER_WIDTH : c.width,
        sortField: field,
        sorter: remote ? true : (a, b, order) => compareValues(sortValueOf(c, a), sortValueOf(c, b), order, isNumeric(c)),
        sortOrder: sort.field === field ? sort.order : null
      }
    })
  }

  /** a-table change 事件的第三个参数（sorter），排序有变化时返回 true（服务端排序时应回到第一页重新查询） */
  function change(sorter) {
    const s = Array.isArray(sorter) ? sorter[0] : sorter
    const order = s?.order || null
    const field = order ? s.column?.sortField || s.columnKey || s.field : null
    if (field === sort.field && order === sort.order) return false
    sort.field = field
    sort.order = order
    return true
  }

  /** 服务端排序的查询参数（没有排序时为空对象） */
  function params() {
    return sort.field && sort.order ? { sortField: sort.field, sortOrder: sort.order === 'descend' ? 'desc' : 'asc' } : {}
  }

  /** 前端排序：按当前排序返回新数组（导出用，与页面顺序一致）；cols 为页面的列定义 */
  function sortRows(rows, cols) {
    if (!sort.field || !sort.order) return rows
    const c = cols.find((x) => x.sortable && sortFieldOf(x) === sort.field)
    if (!c) return rows
    const sign = sort.order === 'ascend' ? 1 : -1
    return [...rows].sort((a, b) => sign * compareValues(sortValueOf(c, a), sortValueOf(c, b), sort.order, isNumeric(c)))
  }

  /** 取消排序（如切换标签页后列不同） */
  function reset() {
    sort.field = null
    sort.order = null
  }

  return { sort, columns, change, params, sortRows, reset }
}

/** 列头排序图标占用的宽度：可排序的列在定义的宽度上加宽，表头文字不被挤成两行 */
const SORTER_WIDTH = 16

/** 列的排序字段：sortField > key > dataIndex */
export const sortFieldOf = (c) => c.sortField || c.key || (Array.isArray(c.dataIndex) ? c.dataIndex.join('.') : c.dataIndex)

const isNumeric = (c) => (c.sortType ? c.sortType === 'number' : c.align === 'right')

function sortValueOf(c, record) {
  if (c.sortValue) return c.sortValue(record)
  const field = c.dataIndex || c.key
  return Array.isArray(field) ? field.reduce((o, k) => o?.[k], record) : record?.[field]
}

const blank = (v) => v === null || v === undefined || v === '' || (typeof v === 'number' && Number.isNaN(v))

let collator
let collatorLocale
function textCompare(a, b) {
  const locale = currentLocale()
  if (!collator || collatorLocale !== locale) {
    collator = new Intl.Collator(locale, { numeric: true, sensitivity: 'base' })
    collatorLocale = locale
  }
  return collator.compare(String(a), String(b))
}

/**
 * 比较两个排序值（升序的结果，Ant Design 降序时取反）：空值排在最后（降序时也在最后）
 *
 * @param order   'ascend' | 'descend'
 * @param numeric 按数值比较（数字字符串也按数值）
 */
export function compareValues(a, b, order, numeric) {
  const ea = blank(a)
  const eb = blank(b)
  if (ea || eb) {
    if (ea && eb) return 0
    const last = ea ? 1 : -1
    return order === 'descend' ? -last : last
  }
  if (numeric) {
    const na = Number(a)
    const nb = Number(b)
    if (!Number.isNaN(na) && !Number.isNaN(nb)) return na - nb
  }
  return textCompare(a, b)
}
