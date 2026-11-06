import { reactive, ref } from 'vue'
import { bizRuleValues } from '@/api/system/customField'

/** 已读取的参数值（同一会话内共用，修改业务参数后由 clearBizRules 清空） */
let cache = {}

/** 业务参数页面保存后、切换账号时清空缓存 */
export function clearBizRules() {
  cache = {}
}

/** 参数值的 JS 表示：true / false 转为布尔，其余保留字符串 */
const parse = (v) => (v === 'true' ? true : v === 'false' ? false : v)

/**
 * 读取业务参数（只能读取后端声明为前端可读的参数，见 BizRuleProvider.publicKeys）：
 *
 *   const { rules, ready } = useBizRules({ 'scm.directDoc.enabled': true })
 *   rules['scm.directDoc.enabled']   // 读取完成前为传入的默认值
 *   await ready                       // 需要等读取完成时
 *
 * @param {Record<string, any>} defaults 参数标识 → 默认值（与后端默认值一致，读取失败时使用）
 * @returns {{ rules: Record<string, any>, loaded: import('vue').Ref<boolean>, ready: Promise<void> }}
 */
export function useBizRules(defaults) {
  const keys = Object.keys(defaults)
  const rules = reactive(Object.fromEntries(keys.map((k) => [k, k in cache ? cache[k] : defaults[k]])))
  const missing = keys.filter((k) => !(k in cache))
  const loaded = ref(!missing.length)
  const ready = missing.length
    ? bizRuleValues(missing)
        .then((values) => {
          missing.forEach((k) => {
            if (values && k in values) {
              cache[k] = parse(values[k])
              rules[k] = cache[k]
            }
          })
        })
        .catch(() => {})
        .finally(() => (loaded.value = true))
    : Promise.resolve()
  return { rules, loaded, ready }
}
