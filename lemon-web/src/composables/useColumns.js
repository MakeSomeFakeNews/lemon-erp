import { computed, ref, watch } from 'vue'
import { load, save } from '@/utils/storage'

export const columnKey = (c) => c.key || c.dataIndex

/** 没有写宽度的列（自适应剩余宽度，如备注）按此最小宽度计入横向滚动宽度 */
const DEFAULT_WIDTH = 80

/** 列宽合计：表格的 scroll.x 用它，与列定义保持一致（勾选列另加 32） */
export const sumWidth = (columns) => columns.reduce((sum, c) => sum + (Number(c.width) || DEFAULT_WIDTH), 0)

/**
 * 列设置：checked 为显示中的列 key，配合 TableToolbar 使用。所有列默认显示，不靠默认隐藏 / 合并字段解决宽度问题
 * （操作列用 components/common/TableActions 的图标按钮，宽度见 actionColumn）。
 * 传入 storageKey 时记住用户在「列设置」中隐藏的列（存 { hidden }，以后新增的列默认显示；兼容旧版本的隐藏列数组）。
 * scrollX 为显示中的列宽合计，用作表格的 scroll.x（有勾选列时 + 32）：
 * 不要写死数字：写死的值比列宽合计大时各列被拉宽，用户隐藏列后表格仍然横向滚动
 */
export function useColumns(columns, storageKey) {
  const allKeys = columns.map(columnKey)
  const cacheKey = storageKey ? `table-hidden-columns:${storageKey}` : null
  const saved = cacheKey ? load(cacheKey, null) : null
  const hidden = new Set(Array.isArray(saved) ? saved : saved?.hidden || [])
  const checked = ref(allKeys.filter((k) => !hidden.has(k)))
  if (cacheKey) {
    watch(checked, (keys) => save(cacheKey, { hidden: allKeys.filter((k) => !keys.includes(k)) }))
  }
  const visibleColumns = computed(() => columns.filter((c) => checked.value.includes(columnKey(c))))
  const scrollX = computed(() => sumWidth(visibleColumns.value))
  return { checked, visibleColumns, scrollX }
}
