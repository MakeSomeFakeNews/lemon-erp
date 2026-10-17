/** 数据大屏的通用工具：设计尺寸、配色、数字格式、图表基础配置 */
import { isZh, t, tt } from '@/locales'
import { formatDecimal, num } from '@/utils/number'

import { ScreenKey } from '@/constants/enums'
/** 设计稿尺寸，页面按比例缩放铺满屏幕 */
export const DESIGN_WIDTH = 1920
export const DESIGN_HEIGHT = 1080

export const COLORS = {
  cyan: '#00e4ff',
  blue: '#3d8bff',
  green: '#2fe6a5',
  orange: '#ffb03a',
  red: '#ff5b5b',
  purple: '#a77bff',
  text: 'rgba(225, 238, 255, 0.92)',
  sub: 'rgba(160, 190, 230, 0.7)'
}
export const PALETTE = [COLORS.cyan, COLORS.green, COLORS.orange, COLORS.purple, COLORS.blue, COLORS.red, '#ff7ad9', '#7cf06b']

export const SCREENS = [
  { key: 'FACTORY', label: t('screen.screens.factory'), desc: t('screen.screens.factoryDesc') },
  { key: 'PROGRESS', label: t('screen.screens.progress'), desc: t('screen.screens.progressDesc') },
  { key: 'WAREHOUSE', label: t('screen.screens.warehouse'), desc: t('screen.screens.warehouseDesc') },
  { key: 'SALES', label: t('screen.screens.sales'), desc: t('screen.screens.salesDesc') },
  { key: 'QUALITY', label: t('screen.screens.quality'), desc: t('screen.screens.qualityDesc') }
]
/** 可以只看某个工作中心的大屏 */
export const WORK_CENTER_SCREENS = [ScreenKey.FACTORY, ScreenKey.PROGRESS]
/** 后端按字典关联出的中文名称：按原始值取界面语言的字典译文，没有值或译文时用原名称 */
export const dictLabel = (type, value, label) => (value === null || value === undefined || value === '' ? label : tt(`dict.${type}.${value}`, label))
/** 列表中某个字段按字典翻译：rows 中 labelKey ← valueKey */
export const withDictLabel = (rows, type, valueKey, labelKey) => (rows || []).map((r) => ({ ...r, [labelKey]: dictLabel(typeof type === 'function' ? type(r) : type, r[valueKey], r[labelKey]) }))
export const screenLabel = (key) => SCREENS.find((s) => s.key === key)?.label || key

/** 数量：千分位，最多 digits 位小数 */
export const fmtNum = (v, digits = 0) => num(v, digits)

/** 金额的显示单位：中文一万以上为「万元」；英文一百万以上为 M、一千以上为 K（不用「万」，英文没有这个计数单位） */
export function moneyUnit(v) {
  const a = Math.abs(Number(v) || 0)
  if (isZh()) return a >= 1e4 ? { div: 1e4, unit: t('screen.unit.tenKYuan') } : { div: 1, unit: t('screen.unit.yuan') }
  if (a >= 1e6) return { div: 1e6, unit: t('screen.unit.mYuan') }
  if (a >= 1e3) return { div: 1e3, unit: t('screen.unit.kYuan') }
  return { div: 1, unit: t('screen.unit.yuan') }
}

/** 图表中金额序列统一的单位（一组数据用同一个单位）：中文万元，英文千元（K） */
export const chartMoneyUnit = () => (isZh() ? { div: 1e4, unit: t('screen.unit.tenKYuan') } : { div: 1e3, unit: t('screen.unit.kYuan') })

/** 金额：按 moneyUnit 缩放，最多两位小数 */
export function fmtMoney(v) {
  if (v === null || v === undefined) return { value: '-', unit: '' }
  const n = Number(v)
  const u = moneyUnit(n)
  return { value: formatDecimal(n / u.div, { max: 2 }), unit: u.unit }
}

export const fmtRate = (v) => (v === null || v === undefined ? '-' : `${Number(v).toFixed(1)}`)
export const shortDay = (d) => (d ? String(d).slice(5) : '')
export const shortTime = (t) => (t ? String(t).replace('T', ' ').slice(5, 16) : '')

/** 坐标轴、提示框的大屏样式 */
export const axisStyle = {
  axisLabel: { color: COLORS.sub, fontSize: 13 },
  axisLine: { lineStyle: { color: 'rgba(80, 160, 255, 0.3)' } },
  splitLine: { lineStyle: { color: 'rgba(80, 160, 255, 0.12)' } }
}
export const tooltip = (extra = {}) => ({
  trigger: 'axis',
  backgroundColor: 'rgba(6, 22, 48, 0.92)',
  borderColor: 'rgba(0, 228, 255, 0.4)',
  textStyle: { color: COLORS.text },
  ...extra
})
export const legend = (extra = {}) => ({ top: 0, right: 0, textStyle: { color: COLORS.sub, fontSize: 13 }, itemWidth: 14, itemHeight: 8, ...extra })

/** 纵向渐变色 */
export const gradient = (color, alpha = 0.15) => ({
  type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
  colorStops: [{ offset: 0, color }, { offset: 1, color: color + Math.round(alpha * 255).toString(16).padStart(2, '0') }]
})
/** 横向渐变色（横向柱状图） */
export const hGradient = (from, to) => ({ type: 'linear', x: 0, y: 0, x2: 1, y2: 0, colorStops: [{ offset: 0, color: from }, { offset: 1, color: to }] })
