import { listPrintTemplates } from '@/api/system/printTemplate'

/**
 * 打印模板：可配置的单据（目录）、已保存的模板配置，以及把模板套用到单据上。
 *
 * 各页面打印时仍按原样组装单据（printDocs 的 doc 结构），传入模板标识后按模板调整：
 * 表头字段按「名称」、明细列按「标题」、签字栏按「名称」匹配，可隐藏、调整顺序、改名，明细列可设宽度。
 * 模板中没有的字段（如之后新增的）按默认显示在末尾。
 *
 * 模板配置结构：
 * {
 *   title, showCompany, showNote, orientation: 'portrait' | 'landscape',
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

// ---------------------------------------------------------------- 套用模板

/** 按配置调整一组条目：getName 取条目名称；返回新的条目数组（已隐藏的去掉，按配置排序，未配置的放末尾） */
function arrange(items, settings, getName, rename) {
  if (!settings?.length) return items
  const byName = new Map(items.map((it) => [getName(it), it]))
  const result = []
  const used = new Set()
  for (const s of settings) {
    const name = s.label ?? s.title
    const it = byName.get(name)
    if (!it || used.has(name)) continue
    used.add(name)
    if (!s.hidden) result.push(s.alias ? rename(it, s) : it)
  }
  for (const it of items) if (!used.has(getName(it))) result.push(it)
  return result
}

const signName = (s) => [].concat(s)[0]

/**
 * 把模板套用到一张单据上（printDocs 的 doc 结构）
 *
 * @param {object} doc
 * @param {object} [tpl] 模板配置
 * @param {object} [common] 通用设置
 */
export function applyTemplate(doc, tpl, common) {
  const result = { ...doc }
  if (common?.companyName) result.companyName = common.companyName
  if (!tpl) return result
  if (tpl.title) result.title = tpl.title
  if (tpl.showCompany === false) result.companyName = ''
  if (tpl.showNote === false) result.note = undefined
  result.orientation = tpl.orientation
  result.fields = arrange(doc.fields || [], tpl.fields, (f) => f[0], (f, s) => [s.alias, ...f.slice(1)])
  result.columns = arrange(doc.columns || [], tpl.columns, (c) => c.title, (c, s) => ({ ...c, title: s.alias }))
  if (tpl.columns?.length) {
    const widths = new Map(tpl.columns.filter((s) => s.width).map((s) => [s.alias || s.title, s.width]))
    result.columns = result.columns.map((c) => (widths.has(c.title) ? { ...c, width: widths.get(c.title) } : c))
  }
  result.signs = arrange(doc.signs || [], tpl.signs, signName, (s, cfg) => {
    const [, value] = [].concat(s)
    return value === undefined ? cfg.alias : [cfg.alias, value]
  })
  return result
}

/** 预览用的示例单据：按目录生成表头、3 行明细和签字栏 */
export function sampleDoc(type) {
  const sample = (title, i) => (title === '序号' ? i + 1 : /数量|金额|单价|用量|已领|补料|税率/.test(title) ? '100.00' : `示例${title}`)
  return {
    title: type.name,
    no: 'NO20260101 0001',
    slip: type.slip,
    fields: type.fields.map((f) => [f, `示例${f}`]),
    columns: type.columns.map((c) => ({ ...c, value: (r, i) => sample(c.title, i) })),
    rows: type.columns.length ? [{}, {}, {}] : [],
    note: type.note ? '示例备注' : undefined,
    signs: type.signs.map((s) => [s, '张三'])
  }
}
