import { createI18n } from 'vue-i18n'
import { load, save } from '@/utils/storage'

/**
 * 界面多语言（vue-i18n，Composition API 模式）。
 * - 语言包按模块拆分：locales/<语言>/<模块>.js 默认导出对象，键为 <模块>.<页面>.<文字>，如 t('fin.voucher.title')
 * - 全部语言包在构建时打包（切换语言后刷新页面，模块顶层用 t() 定义的列、选项等无需响应式）
 * - menu.js、dict.js 是菜单（按菜单 ID）、字典（按类型和值）的译文，中文直接用数据库中的名称，没有译文时显示原名称
 * - 插值写 {name}；文字中的 @ { } | $ 是 vue-i18n 的特殊字符，要写成 {'@'} 这样的字面量
 */
export const LOCALES = [
  { value: 'zh-CN', label: '简体中文', short: '中' },
  { value: 'en-US', label: 'English', short: 'EN' }
]
export const DEFAULT_LOCALE = 'zh-CN'
const LOCALE_KEY = 'lemon_erp_locale'

const modules = import.meta.glob('./*/*.js', { eager: true })
const messages = {}
for (const [path, mod] of Object.entries(modules)) {
  const [, lang, name] = path.match(/^\.\/([^/]+)\/([^/]+)\.js$/)
  ;(messages[lang] ||= {})[name] = mod.default
}

/** 初始语言：上次选择的语言；没有选择过时，英文浏览器用英文，其他用中文 */
function initialLocale() {
  const saved = load(LOCALE_KEY, null)
  if (LOCALES.some((l) => l.value === saved)) return saved
  return navigator.language?.toLowerCase().startsWith('en') ? 'en-US' : DEFAULT_LOCALE
}

export const i18n = createI18n({
  legacy: false,
  globalInjection: true,
  locale: initialLocale(),
  fallbackLocale: DEFAULT_LOCALE,
  messages,
  missingWarn: false,
  fallbackWarn: false
})

/** 当前语言，如 zh-CN */
export const currentLocale = () => i18n.global.locale.value
export const isZh = () => currentLocale() === 'zh-CN'

/** 在组件之外（js 模块、store、模块顶层常量）翻译 */
export const t = (...args) => i18n.global.t(...args)
export const te = (key) => i18n.global.te(key) || i18n.global.te(key, DEFAULT_LOCALE)

/** 有译文时翻译，否则返回 fallback（菜单、字典等数据库中的名称） */
export function tt(key, fallback) {
  return i18n.global.te(key) ? i18n.global.t(key) : fallback
}

/** 切换语言：保存后刷新页面 */
export function setLocale(locale) {
  save(LOCALE_KEY, locale)
  location.reload()
}

document.documentElement.lang = currentLocale()
