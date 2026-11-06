import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getAiStatus } from '@/api/ai/config'
import { useUserStore } from '@/store/user'
import { asPrefilled } from './useFormDrawer'

/**
 * 智能录入的预填：「智能录入」页面识别后跳转到业务页面（?ai=表单标识），或在业务页面上用「AI 识别」按钮直接识别，
 * 打开新增表单并填入识别结果，表单上方列出需要核对的提示，有提示的字段加上 ai-warn 样式（黄色边框）。
 *
 * 跨页面传递的识别结果只保存在内存中（不写 sessionStorage / localStorage，刷新页面即丢弃，不会残留在浏览器里），
 * 10 分钟内有效、只能使用一次。
 *
 * 页面接入：
 *   const ai = useAiPrefill('supplier', (data) => drawer.openCreate({ ...data }), () => drawer.open.value)
 *   <AiExtractButton target="scm.supplier" @result="ai.apply" />      工具栏
 *   <AiPrefillAlert :ai="ai" />                                        表单顶部
 *   <a-form-item :class="ai.cls('partnerId')">                         需要高亮的字段（可选）
 */

const TTL = 10 * 60 * 1000
let pending = null

/** 「智能录入」页面跳转前保存识别结果 */
export function setPendingPrefill(result) {
  pending = { form: result.form, result, at: Date.now() }
}

function takePending(form) {
  const p = pending
  if (!p || p.form !== form || Date.now() - p.at > TTL) return null
  pending = null
  return p.result
}

let statusPromise
let statusAt = 0
/** 当前租户的 AI 状态（各页面共用，缓存 1 分钟，AI 设置修改后很快生效；失败时下次重试） */
export function loadAiStatus() {
  if (!statusPromise || Date.now() - statusAt > 60000) {
    statusAt = Date.now()
    statusPromise = getAiStatus().catch((e) => {
      statusPromise = undefined
      throw e
    })
  }
  return statusPromise
}

/** 有 ai:extract:use 权限且租户开启了智能录入时为 true */
export function useAiExtractEnabled() {
  const enabled = ref(false)
  const userStore = useUserStore()
  onMounted(async () => {
    if (!userStore.hasPerm('ai:extract:use')) return
    const status = await loadAiStatus().catch(() => null)
    enabled.value = !!status?.features?.EXTRACT
  })
  return enabled
}

/**
 * @param {string}   form     表单标识（与后端 AiExtractTarget.form() 一致）
 * @param {Function} open     (data, result) => 打开新增表单并填入数据（须同步调用 useFormDrawer 的 openCreate，打开的表单视为已修改）
 * @param {Function} [visible] () => 表单是否打开；关闭后清除提示
 */
export function useAiPrefill(form, open, visible) {
  const route = useRoute()
  const router = useRouter()
  const warnings = ref([])
  const source = ref(null)

  async function apply(result) {
    if (!result || result.form !== form) return
    // 预填的表单视为已修改：直接关闭时提示放弃，避免丢掉识别结果
    await asPrefilled(() => open(result.data || {}, result))
    // 打开表单时可能触发 visible 的变化，填入后再设置提示
    warnings.value = result.warnings || []
    source.value = result
  }

  function clear() {
    warnings.value = []
    source.value = null
  }

  /** 字段路径（如 partnerId、items[0].materialId）有提示时返回 'ai-warn' */
  const cls = (field) => (warnings.value.some((w) => w.field === field) ? 'ai-warn' : undefined)

  if (visible) {
    watch(visible, (v) => {
      if (!v) clear()
    })
  }

  onMounted(() => {
    if (route.query.ai !== form) return
    const { ai, ...rest } = route.query
    router.replace({ query: rest })
    const result = takePending(form)
    if (result) apply(result)
  })

  // reactive：模板中直接用 ai.warnings、ai.cls(...)，可以作为 prop 传给表单子组件
  return reactive({ warnings, source, apply, clear, cls })
}
