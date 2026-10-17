/** 数据大屏的通用工具：设计尺寸、配色、数字格式、图表基础配置 */

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
  { key: 'FACTORY', label: '生产车间', desc: '今日产量与良品率、在制工单进度、设备状态、工作中心负荷、实时报工、停机原因' },
  { key: 'WAREHOUSE', label: '仓储物流', desc: '今日出入库、待拣货与待发货、出入库趋势、各仓库库存、库存预警与临期批次、实时出入库' },
  { key: 'SALES', label: '销售经营', desc: '订单与出库金额、回款与应收、月度趋势与同比、客户和产品排行、业务员排行、实时订单' },
  { key: 'QUALITY', label: '质量管理', desc: '检验合格率与趋势、来料与过程质量、不良类型柏拉图、供应商质量排名、待处置不合格品' }
]
export const screenLabel = (key) => SCREENS.find((s) => s.key === key)?.label || key

/** 数量：千分位，最多 digits 位小数 */
export function fmtNum(v, digits = 0) {
  if (v === null || v === undefined || v === '') return '-'
  const n = Number(v)
  if (Number.isNaN(n)) return '-'
  return n.toLocaleString('zh-CN', { maximumFractionDigits: digits, minimumFractionDigits: 0 })
}

/** 金额：一万以上显示为「万」 */
export function fmtMoney(v) {
  if (v === null || v === undefined) return { value: '-', unit: '' }
  const n = Number(v)
  if (Math.abs(n) >= 10000) return { value: (n / 10000).toLocaleString('zh-CN', { maximumFractionDigits: 2 }), unit: '万元' }
  return { value: n.toLocaleString('zh-CN', { maximumFractionDigits: 2 }), unit: '元' }
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
