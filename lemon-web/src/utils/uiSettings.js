import { LOCALES } from '@/locales'

/**
 * 界面设置（每个用户一份，保存在服务端 /profile/settings/ui，本地 localStorage 作为缓存）：
 * 主题模式、主题色、布局、多标签页、紧凑模式、侧边栏折叠，以及界面语言（见 store/app.js 的说明）
 */

/** 主题模式 */
export const ThemeMode = { LIGHT: 'light', DARK: 'dark', SYSTEM: 'system' }

/**
 * 布局：
 * - mix    顶部模块 + 侧边菜单（默认）：顶栏切换一级模块，侧边栏显示当前模块的分组和页面
 * - side   侧边菜单：全部模块、分组、页面以多级树显示在左侧，顶栏只有面包屑、搜索、消息、用户等
 * - top    顶部菜单：全部菜单以水平菜单显示在顶部（模块下拉展开分组和页面），没有侧边栏
 * - column 双栏：最左一列显示模块图标和名称，第二栏显示当前模块的分组和页面
 * 小于 768px（手机）时统一使用抽屉菜单，不受布局影响
 */
export const LayoutMode = { MIX: 'mix', SIDE: 'side', TOP: 'top', COLUMN: 'column' }
export const LAYOUTS = [LayoutMode.MIX, LayoutMode.SIDE, LayoutMode.TOP, LayoutMode.COLUMN]

/** 品牌色（拂晓蓝），也是默认主题色 */
export const BRAND_COLOR = '#1677ff'

/** 预设主题色：key 对应语言包 layout.settings.colors.<key> */
export const PRESET_COLORS = [
  { key: 'daybreak', color: BRAND_COLOR },
  { key: 'dust', color: '#f5222d' },
  { key: 'volcano', color: '#fa541c' },
  { key: 'sunset', color: '#faad14' },
  { key: 'lemon', color: '#d4b106' },
  { key: 'polar', color: '#52c41a' },
  { key: 'cyan', color: '#13c2c2' },
  { key: 'geekblue', color: '#2f54eb' },
  { key: 'purple', color: '#722ed1' },
  { key: 'magenta', color: '#eb2f96' }
]

export const DEFAULT_SETTINGS = Object.freeze({
  themeMode: ThemeMode.SYSTEM,
  primaryColor: BRAND_COLOR,
  layout: LayoutMode.MIX,
  tabs: true,
  compact: false,
  collapsed: false
})

/** 「恢复默认」时保留的项：侧边栏折叠只是临时状态，语言单独切换 */
export const KEEP_ON_RESET = ['collapsed', 'locale']

const COLOR_RE = /^#[0-9a-f]{6}$/i
const bool = (v, fallback) => (typeof v === 'boolean' ? v : fallback)

/**
 * 规范化（本地缓存、服务端返回的内容都可能来自旧版本或被修改过）：不认识的项丢弃，非法取值用默认值。
 * 兼容旧版本本地缓存 lemon_erp_settings 的 { dark, collapsed }
 */
export function normalizeSettings(raw) {
  const s = raw && typeof raw === 'object' && !Array.isArray(raw) ? raw : {}
  const legacyMode = typeof s.dark === 'boolean' ? (s.dark ? ThemeMode.DARK : ThemeMode.LIGHT) : DEFAULT_SETTINGS.themeMode
  const result = {
    themeMode: Object.values(ThemeMode).includes(s.themeMode) ? s.themeMode : legacyMode,
    primaryColor: COLOR_RE.test(s.primaryColor) ? s.primaryColor.toLowerCase() : DEFAULT_SETTINGS.primaryColor,
    layout: LAYOUTS.includes(s.layout) ? s.layout : DEFAULT_SETTINGS.layout,
    tabs: bool(s.tabs, DEFAULT_SETTINGS.tabs),
    compact: bool(s.compact, DEFAULT_SETTINGS.compact),
    collapsed: bool(s.collapsed, DEFAULT_SETTINGS.collapsed)
  }
  if (LOCALES.some((l) => l.value === s.locale)) result.locale = s.locale
  return result
}

/** 是否与默认设置相同（语言不算） */
export function isDefaultSettings(s) {
  return Object.keys(DEFAULT_SETTINGS).every((k) => s[k] === DEFAULT_SETTINGS[k])
}
