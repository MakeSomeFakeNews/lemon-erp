import { computed, unref } from 'vue'
import { round2, sumBy } from '@/utils/number'

/** 明细行的唯一 key（新增行没有 id） */
let seq = 0
export const rowKey = () => `row-${Date.now()}-${++seq}`

/** 已保存的明细用 id 作 key，新增行生成临时 key */
export const lineKey = (item) => (item.id != null ? `item-${item.id}` : rowKey())

/**
 * 单据明细行编辑的通用逻辑（增删行、行 key、合计），表格用 <LineItemsTable>：
 *
 *   const lines = useLineItems(() => form.value.items, { newRow: () => ({ materialId: undefined, qty: undefined, taxRate: 13 }) })
 *   lines.addRow()                       // 末尾加一行（{ first: true } 加在最前）
 *   const total = lines.total('amount')  // computed，合计按 round2 舍入
 *
 * @param {Function|import('vue').Ref<Array>|Array} items 明细数组或返回它的函数（表单对象会整体替换时用函数）
 * @param {object}   [options]
 * @param {Function} [options.newRow] (当前明细) => 新行的默认值，不需要带 key
 * @param {string}   [options.field='items'] 明细在表单中的字段名，用于 itemName 生成表单校验路径
 */
export function useLineItems(items, { newRow, field = 'items' } = {}) {
  const list = () => unref(typeof items === 'function' ? items() : items) || []

  /** 加一行并返回该行；extra 覆盖默认值 */
  function addRow(extra = {}, { first = false } = {}) {
    const arr = list()
    const row = { ...(newRow ? newRow(arr) : {}), ...extra }
    if (row.key == null) row.key = rowKey()
    first ? arr.unshift(row) : arr.push(row)
    return row
  }

  /** 删除行：传行对象（表格分页时 index 不是在明细中的位置）或下标 */
  function removeRow(target) {
    const arr = list()
    const i = typeof target === 'number' ? target : arr.indexOf(target)
    if (i >= 0) arr.splice(i, 1)
  }

  /** 给后端返回的明细补上 key */
  const withKey = (item) => ({ ...item, key: lineKey(item) })

  /** 合计（computed）：字段名或 (行) => 值，round 默认 round2，传 (n) => n 不舍入 */
  const total = (by, round = round2) => computed(() => sumBy(list(), by, round))

  /** 行内表单项的校验路径 ['items', 行位置, 字段] */
  const itemName = (record, name) => [field, list().indexOf(record), name]

  return { addRow, removeRow, withKey, total, itemName }
}
