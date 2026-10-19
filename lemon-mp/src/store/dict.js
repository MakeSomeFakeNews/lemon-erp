import { reactive } from 'vue'
import { get } from '@/utils/request'
import { dictLabel } from '@/locales'

/**
 * 字典缓存：同一类型只请求一次。const dict = useDict('crm_customer_level')，模板中 dict.crm_customer_level 为 [{ label, value, tagColor }]；
 * 标签按界面语言翻译（与 Web 端共用英文译文），用 dictText(dict.xxx, value) 取标签
 */
const cache = reactive({})
const pending = new Map()

function load(type) {
  if (cache[type]) return Promise.resolve(cache[type])
  if (!pending.has(type)) {
    pending.set(
      type,
      get(`/system/dict/data/type/${type}`, null, { silent: true })
        .then((list) => {
          cache[type] = (list || []).map((d) => ({ ...d, label: dictLabel(type, d.value, d.label) }))
          return cache[type]
        })
        .finally(() => pending.delete(type))
    )
  }
  return pending.get(type)
}

export function useDict(...types) {
  types.forEach((t) => load(t).catch(() => {}))
  return new Proxy(
    {},
    {
      get: (_, type) => cache[type] || []
    }
  )
}

export function dictText(list, value) {
  if (value === null || value === undefined || value === '') return ''
  return (list || []).find((d) => String(d.value) === String(value))?.label ?? String(value)
}

/** 字典项的 tagColor（Web 端 ant-design-vue 的颜色名） */
export function dictColor(list, value) {
  return (list || []).find((d) => String(d.value) === String(value))?.tagColor || ''
}

export function clearDict() {
  Object.keys(cache).forEach((k) => delete cache[k])
}
