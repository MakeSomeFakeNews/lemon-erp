import { computed, reactive } from 'vue'
import { useDictStore } from '@/store/dict'

/**
 * 按类型加载字典：const dict = useDict('sys_common_status')，模板中使用 dict.sys_common_status
 */
export function useDict(...types) {
  const store = useDictStore()
  types.forEach((t) => store.load(t).catch(() => {}))
  return reactive(Object.fromEntries(types.map((t) => [t, computed(() => store.cache[t] || [])])))
}
