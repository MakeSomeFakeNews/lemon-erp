import { ref } from 'vue'
import webDictEn from '@web/locales/en-US/dict.js'

/**
 * 界面多语言（简体中文 / 英文）。小程序不能运行 vue-i18n 的消息编译（没有 eval），这里是一个最小实现：
 * - 语言包按模块拆分：locales/<语言>/<模块>.js 默认导出对象，键为 <模块>.<文字>，如 t('approval.approve')
 * - 插值写 {name}，t('common.total', { n: 3 })
 * - locale 是响应式的，模板中的 t() 在切换语言后自动更新；底部导航由 App 按语言设置
 * - 字典的英文译文与 Web 端共用（lemon-web/src/locales/en-US/dict.js），中文直接使用数据库中的标签
 */
export const LOCALES = [
  { value: 'zh-CN', label: '简体中文' }, // i18n-ignore 语言名称
  { value: 'en-US', label: 'English' }
]
export const DEFAULT_LOCALE = 'zh-CN'
const LOCALE_KEY = 'lemon_mp_locale'

const modules = import.meta.glob('./*/*.js', { eager: true })
const messages = {}
for (const [path, mod] of Object.entries(modules)) {
  const [, lang, name] = path.match(/^\.\/([^/]+)\/([^/]+)\.js$/)
  ;(messages[lang] ||= {})[name] = mod.default
}

function initialLocale() {
  const saved = uni.getStorageSync(LOCALE_KEY)
  if (LOCALES.some((l) => l.value === saved)) return saved
  const sys = (uni.getSystemInfoSync().language || '').toLowerCase()
  return sys.startsWith('en') ? 'en-US' : DEFAULT_LOCALE
}

export const locale = ref(initialLocale())
export const isZh = () => locale.value === 'zh-CN'

function lookup(lang, key) {
  let v = messages[lang]
  for (const part of key.split('.')) {
    if (v == null) return undefined
    v = v[part]
  }
  return typeof v === 'string' ? v : undefined
}

/** 翻译：没有译文时回退到中文，再没有时返回键本身 */
export function t(key, params) {
  const msg = lookup(locale.value, key) ?? lookup(DEFAULT_LOCALE, key) ?? key
  if (!params) return msg
  return msg.replace(/\{(\w+)\}/g, (m, name) => (params[name] === undefined || params[name] === null ? m : String(params[name])))
}

/** 是否有该键（任一语言） */
export const te = (key) => lookup(locale.value, key) !== undefined || lookup(DEFAULT_LOCALE, key) !== undefined

/** 字典标签：英文按 Web 端的译文，中文用数据库中的标签 */
export function dictLabel(type, value, fallback) {
  if (isZh()) return fallback
  return webDictEn[type]?.[String(value)] ?? fallback
}

export function setLocale(value) {
  locale.value = value
  uni.setStorageSync(LOCALE_KEY, value)
}

/** 在 setup 中使用：const { t } = useI18n() */
export function useI18n() {
  return { t, te, locale }
}
