import { reactive } from 'vue'
import { get } from '@/utils/request'

/**
 * 业务参数（与 Web 端 composables/useBizRules 相同的接口 /system/biz-rule/values，只能读取后端声明为前端可读的参数）。
 * 同一会话内缓存，切换账号时由 resetUser 清空。
 *
 *   await loadBizRules(['scm.directDoc.enabled'])
 *   bizRule('scm.directDoc.enabled', true)   // 未读取或读取失败时为默认值（与后端默认值一致）
 */
const state = reactive({})

/** 参数值的 JS 表示：true / false 转为布尔，其余保留字符串 */
const parse = (v) => (v === 'true' ? true : v === 'false' ? false : v)

/** 读取尚未缓存的参数；失败时不抛出（使用默认值） */
export async function loadBizRules(keys) {
  const missing = keys.filter((k) => !(k in state))
  if (!missing.length) return
  const values = await get('/system/biz-rule/values', { keys: missing }, { silent: true }).catch(() => null)
  missing.forEach((k) => values && k in values && (state[k] = parse(values[k])))
}

/** 响应式读取，在 computed / 模板中使用时读取完成后自动更新 */
export const bizRule = (key, defaultValue) => (key in state ? state[key] : defaultValue)

export function clearBizRules() {
  Object.keys(state).forEach((k) => delete state[k])
}
