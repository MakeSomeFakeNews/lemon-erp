import { computed, ref, watch } from 'vue'
import { load, save } from '@/utils/storage'

export const columnKey = (c) => c.key || c.dataIndex

/**
 * 列设置：checked 为显示中的列 key，配合 TableToolbar 使用。
 * 传入 storageKey 时记住用户隐藏的列（只存隐藏的列，以后新增的列默认显示）。
 */
export function useColumns(columns, storageKey) {
  const allKeys = columns.map(columnKey)
  const cacheKey = storageKey ? `table-hidden-columns:${storageKey}` : null
  const hidden = new Set(cacheKey ? load(cacheKey, []) : [])
  const checked = ref(allKeys.filter((k) => !hidden.has(k)))
  if (cacheKey) {
    watch(checked, (keys) => save(cacheKey, allKeys.filter((k) => !keys.includes(k))))
  }
  const visibleColumns = computed(() => columns.filter((c) => checked.value.includes(columnKey(c))))
  return { checked, visibleColumns }
}
