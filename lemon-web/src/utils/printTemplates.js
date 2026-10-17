import { listPrintTemplates } from '@/api/system/printTemplate'

/**
 * 打印模板：可配置的单据（目录）、已保存的模板配置，以及把模板套用到单据上。
 *
 * 各页面打印时仍按原样组装单据（printDocs 的 doc 结构），传入模板标识后按模板调整：
 * 表头字段按「名称」、明细列按「标题」、签字栏按「名称」匹配，可隐藏、调整顺序、改名，明细列可设宽度。
 * 模板中没有的字段（如之后新增的）按默认显示在末尾。
 *
 * 打印由后端用 JasperReports 生成 PDF（见 utils/print.js）：按这里的配置生成默认版式；
 * 上传了 JRXML 的单据（配置中带 jrxml: true）按 JRXML 打印，字段、列配置不再生效。
 *
 * 模板配置结构：
 * {
 *   title, showCompany, showNote, orientation: 'portrait' | 'landscape',
 *   paper: PAPERS 的 value，paperWidth / paperHeight（自定义纸张，mm），margin（页边距 mm），fontSize（pt）,
 *   fields:  [{ label, alias, hidden }],
 *   columns: [{ title, alias, hidden, width }]   // width 单位 mm
 *   signs:   [{ label, alias, hidden }]
 * }
 * 通用设置（模板标识 _common）：{ companyName }
 */
export const COMMON_KEY = '_common'

const MATERIAL_COLUMNS = ['序号', '物料编码', '物料名称', '规格型号', '单位']
const num = (title) => ({ title, align: 'right' })

const order = (key, name, partner, warehouse) => ({
  key,
  module: '供应链',
  name,
  fields: [partner, '订单日期', '交货日期', warehouse, '价税合计', '税额', '币种 / 汇率', '版本'],
  columns: [...MATERIAL_COLUMNS, num('数量'), num('含税单价'), num('税率'), num('价税合计'), '交货日期'],
  signs: ['制单', '审核', `${partner}确认`],
  note: true
})
const trade = (key, name, partner) => ({
  key,
  module: '供应链',
  name,
  fields: ['订单号', partner, '仓库', '业务日期', '仓库单据', '价税合计', '原币'],
  columns: [...MATERIAL_COLUMNS, '库位', '批次', num('数量'), num('含税单价'), num('价税合计'), '序列号'],
  signs: ['制单', '仓管', `${partner}签收`],
  note: true
})
const stock = (key, name, warehouse, transfer) => ({
  key,
  module: '仓库',
  name,
  fields: [warehouse, ...(transfer ? ['调入仓库'] : []), '业务日期', '来源单号', '合计数量'],
  columns: [...MATERIAL_COLUMNS, transfer ? '调出库位' : '库位', ...(transfer ? ['调入库位'] : []), '批次', num('数量'), '序列号'],
  signs: ['制单', '确认', '经手人'],
  note: true
})

/** 可配置打印模板的单据：名称、表头字段、明细列、签字栏与各页面打印时一致 */
export const PRINT_TYPES = [
  order('scm.purchaseOrder', '采购订单', '供应商', '收货仓库'),
  order('scm.salesOrder', '销售订单', '客户', '发货仓库'),
  trade('scm.purchaseIn', '采购入库单', '供应商'),
  trade('scm.purchaseReturn', '采购退货单', '供应商'),
  trade('scm.salesOut', '销售出库单', '客户'),
  trade('scm.salesReturn', '销售退货单', '客户'),
  {
    key: 'scm.pickList',
    module: '供应链',
    name: '拣货单',
    fields: ['销售订单', '客户', '发货仓库', '发货日期', '制单', '状态'],
    columns: ['序号', '物料', '库位', '批次', '有效期', num('数量'), '序列号'],
    signs: ['制单', '拣货', '复核'],
    note: true
  },
  {
    key: 'scm.wavePick',
    module: '供应链',
    name: '波次拣货单',
    fields: ['仓库', '发货通知', '制单', '状态'],
    columns: ['序号', '库位', '物料', '批次', '有效期', num('数量'), '分拣'],
    signs: ['制单', '拣货', '复核'],
    note: true
  },
  {
    key: 'scm.packingList',
    module: '供应链',
    name: '装箱单',
    fields: ['销售订单', '客户', '发货仓库', '发货日期', '箱数', '总毛重', '总体积'],
    columns: ['箱号', '物料', num('数量'), num('毛重'), num('体积')],
    signs: ['装箱', '复核', '客户签收'],
    note: true
  },
  stock('wms.in', '入库单', '入库仓库'),
  stock('wms.out', '出库单', '出库仓库'),
  stock('wms.transfer', '调拨单', '调出仓库', true),
  {
    key: 'fin.voucher',
    module: '财务',
    name: '记账凭证',
    fields: ['日期', '期间', '附单据'],
    columns: ['摘要', '会计科目', '部门 / 辅助', num('借方金额'), num('贷方金额')],
    signs: ['制单', '审核', '记账', '财务主管']
  },
  {
    key: 'fin.partnerStatement',
    module: '财务',
    name: '往来对账单',
    fields: ['客户', '供应商', '对账期间', '期初余额', '期末余额'],
    columns: ['日期', '类型', '单号', '来源 / 结算方式', num('增加金额'), num('减少金额'), num('余额'), '备注'],
    signs: ['制表', '财务审核', '客户确认（盖章）', '供应商确认（盖章）'],
    note: true
  },
  {
    key: 'mes.workOrder',
    module: '生产',
    name: '生产任务单',
    fields: ['产品', 'BOM 版本', '计划数量', '计划开工', '计划完工', '完工入库仓库'],
    columns: [...MATERIAL_COLUMNS, num('单位用量'), num('需求数量'), num('补料'), num('已领')],
    signs: ['制单', '生产主管', '仓库'],
    note: true
  },
  {
    key: 'hr.payslip',
    module: '人力',
    name: '工资条',
    slip: true,
    fields: ['姓名', '工号', '部门', '出勤 / 工作日', '基本工资', '津贴补贴', '加班费', '计件工资', '绩效奖金', '其他奖金',
      '考勤扣款', '应发工资', '个人社保', '个人公积金', '专项附加扣除', '个税', '其他扣款', '实发工资', '备注']
  }
].map((t) => ({ ...t, columns: (t.columns || []).map((c) => (typeof c === 'string' ? { title: c } : c)), signs: t.signs || [] }))

export const PRINT_TYPE_MAP = Object.fromEntries(PRINT_TYPES.map((t) => [t.key, t]))

// ---------------------------------------------------------------- 已保存的模板

let cache = null

/** 已保存的模板（按租户），首次打印时加载，模板修改后调用 setTemplates 更新 */
export async function loadTemplates(force = false) {
  if (!cache || force) cache = (await listPrintTemplates()) || {}
  return cache
}

export function setTemplates(templates) {
  cache = templates
}

// ---------------------------------------------------------------- 纸张

/** 纸张（mm，纵向的宽 × 高）；连续纸用于针式打印机，241 × 140 也是常用的凭证纸 */
export const PAPERS = [
  { value: 'A4', label: 'A4（210 × 297）', width: 210, height: 297 },
  { value: 'A5', label: 'A5（148 × 210）', width: 148, height: 210 },
  { value: 'B5', label: 'B5（176 × 250）', width: 176, height: 250 },
  { value: '241x280', label: '连续纸一等分（241 × 280）', width: 241, height: 280 },
  { value: '241x140', label: '连续纸二等分 / 凭证纸（241 × 140）', width: 241, height: 140 },
  { value: '241x93', label: '连续纸三等分（241 × 93）', width: 241, height: 93 },
  { value: 'custom', label: '自定义' }
]
const PAPER_MAP = Object.fromEntries(PAPERS.map((p) => [p.value, p]))

export const DEFAULT_MARGIN = 10
export const DEFAULT_FONT_SIZE = 10

/** 模板中的纸张设置换算为打印请求的 layout：{ width, height, margin, fontSize }（mm / pt，已按方向换算） */
export function layoutOf(tpl = {}) {
  const paper = PAPER_MAP[tpl.paper] || PAPER_MAP.A4
  let width = paper.value === 'custom' ? Number(tpl.paperWidth) || 210 : paper.width
  let height = paper.value === 'custom' ? Number(tpl.paperHeight) || 297 : paper.height
  if (tpl.orientation === 'landscape') [width, height] = [height, width]
  return { width, height, margin: tpl.margin ?? DEFAULT_MARGIN, fontSize: tpl.fontSize || DEFAULT_FONT_SIZE }
}

// ---------------------------------------------------------------- 套用模板

/** 按配置调整一组条目（已隐藏的去掉，按配置排序，未配置的放末尾）；条目按 key 匹配配置中的名称 */
function arrange(items, settings, apply) {
  if (!settings?.length) return items
  const byKey = new Map(items.map((it) => [it.key, it]))
  const result = []
  const used = new Set()
  for (const s of settings) {
    const name = s.label ?? s.title
    const it = byKey.get(name)
    if (!it || used.has(name)) continue
    used.add(name)
    if (!s.hidden) result.push(apply(it, s))
  }
  for (const it of items) if (!used.has(it.key)) result.push(it)
  return result
}

/**
 * 把模板套用到一张单据上（toPrintDoc 规范化后的结构）：隐藏、排序、改名、列宽只改显示，key 保持单据中的原名称。
 * 上传了 JRXML 的单据版式由 JRXML 决定，只套用公司名称
 *
 * @param {object} doc
 * @param {object} [tpl] 模板配置
 * @param {object} [common] 通用设置
 */
export function applyTemplate(doc, tpl, common) {
  const result = { ...doc }
  if (common?.companyName) result.companyName = common.companyName
  if (!tpl || tpl.jrxml) return result
  if (tpl.title) result.title = tpl.title
  if (tpl.showCompany === false) result.companyName = ''
  if (tpl.showNote === false) result.note = undefined
  result.fields = arrange(doc.fields || [], tpl.fields, (f, s) => (s.alias ? { ...f, label: s.alias } : f))
  result.columns = arrange(doc.columns || [], tpl.columns, (c, s) => ({
    ...c,
    ...(s.alias ? { title: s.alias } : {}),
    ...(s.width ? { width: s.width } : {})
  }))
  result.signs = arrange(doc.signs || [], tpl.signs, (it, s) => (s.alias ? { ...it, label: s.alias } : it))
  return result
}

/** 预览用的示例单据（规范化后的结构）：按目录生成表头、3 行明细和签字栏 */
export function sampleDoc(type) {
  const sample = (title, i) => (title === '序号' ? String(i + 1) : /数量|金额|单价|用量|已领|补料|税率/.test(title) ? '100.00' : `示例${title}`)
  const item = (name, value) => ({ key: name, label: name, value })
  return {
    title: type.name,
    no: 'NO202601010001',
    slip: !!type.slip,
    fields: type.fields.map((f) => item(f, `示例${f}`)),
    columns: type.columns.map((c) => ({ key: c.title, title: c.title, right: c.align === 'right' })),
    rows: type.columns.length ? [0, 1, 2].map((i) => Object.fromEntries(type.columns.map((c) => [c.title, sample(c.title, i)]))) : [],
    note: type.note ? '示例备注' : undefined,
    signs: type.signs.map((s) => item(s, '张三'))
  }
}
