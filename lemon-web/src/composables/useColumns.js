import { computed, ref, watch } from 'vue'
import { load, save } from '@/utils/storage'
import { useDictStore } from '@/store/dict'
import { textWidth } from '@/components/common/tableActions'

export const columnKey = (c) => c.key || c.dataIndex

/** 没有写宽度的列（自适应剩余宽度，如备注）按此最小宽度计入横向滚动宽度 */
const DEFAULT_WIDTH = 80

/** 列宽合计：表格的 scroll.x 用它，与列定义保持一致（勾选列另加 32） */
export const sumWidth = (columns) => columns.reduce((sum, c) => sum + (Number(c.width) || DEFAULT_WIDTH), 0)

/** 标签左右内边距与边框 16、标签右外边距 8、单元格左右内边距 16、余量 6 */
const TAG_PAD = 46
/** 标签文字超过此宽度时在单词之间换行，不再加宽列 */
const TAG_MAX = 120

/**
 * 字典标签列的宽度：按当前语言最长的标签加宽（英文标签较长），较长的标签只在单词之间换行，不在单词中间断开；
 * 不小于列定义的宽度
 */
export function dictColumnWidth(width, labels = []) {
  if (!labels.length) return width
  const full = Math.max(...labels.map((l) => textWidth(l, 12)))
  const word = Math.max(...labels.flatMap((l) => String(l).split(/\s+/)).map((w) => textWidth(w, 12)))
  return Math.max(Number(width) || 0, Math.ceil(Math.min(full, TAG_MAX) + TAG_PAD), Math.ceil(word + TAG_PAD))
}

/** 表头文字（加粗，按 1.08 倍估算）单行放得下的列宽：英文表头较长时加宽，不折成两行；表头不是文字时不处理 */
const titleWidth = (title) => (typeof title === 'string' && title ? Math.ceil(textWidth(title) * 1.08) + 20 : 0)

/**
 * 列设置：checked 为显示中的列 key，配合 TableToolbar 使用。所有列默认显示，不靠默认隐藏 / 合并字段解决宽度问题
 * （操作列用 components/common/TableActions 的图标按钮，宽度见 actionColumn）。
 * 传入 storageKey 时记住用户在「列设置」中隐藏的列（存 { hidden }，以后新增的列默认显示；兼容旧版本的隐藏列数组）。
 * scrollX 为显示中的列宽合计，用作表格的 scroll.x（有勾选列时 + 32）：
 * 不要写死数字：写死的值比列宽合计大时各列被拉宽，用户隐藏列后表格仍然横向滚动
 * 列定义带 dict（字典标签列，也用于导出）且不是省略列时，宽度按字典标签自动加宽（dictColumnWidth）
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
  const dictStore = useDictStore()
  const dictTypes = [...new Set(columns.filter((c) => c.dict && !c.ellipsis).map((c) => c.dict))]
  dictTypes.forEach((type) => dictStore.load(type).catch(() => {}))
  const sized = computed(() =>
    columns.map((c) => {
      if (!Number(c.width)) return c
      const list = c.dict && !c.ellipsis ? dictStore.cache[c.dict] : null
      const width = Math.max(titleWidth(c.title), list ? dictColumnWidth(c.width, list.map((d) => d.label)) : c.width)
      return width === c.width ? c : { ...c, width }
    })
  )
  const visibleColumns = computed(() => sized.value.filter((c) => checked.value.includes(columnKey(c))))
  const scrollX = computed(() => sumWidth(visibleColumns.value))
  return { checked, visibleColumns, scrollX }
}
