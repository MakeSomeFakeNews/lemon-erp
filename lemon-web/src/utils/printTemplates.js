import { listPrintTemplates } from '@/api/system/printTemplate'
import { isZh, t } from '@/locales'
import zhPrint from '@/locales/zh-CN/print'

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
 *
 * 多语言：字段、列、签字栏的「中文名称」是匹配键（模板配置、上传的 JRXML 都按它匹配），不随界面语言变化；
 * 显示时用 printLabel 翻译（译文在 locales/<语言>/print.js 的 name 中，这里按中文语言包取中文名称）
 */
export const COMMON_KEY = '_common'

/** 打印名称（中文），即匹配键 */
const n = zhPrint.name
const NAME_IDS = Object.fromEntries(Object.entries(n).map(([id, name]) => [name, id]))

/** 打印名称（中文匹配键）在当前语言下的显示文字；不是登记的名称时原样返回 */
export function printLabel(name) {
  const id = NAME_IDS[name]
  return id && !isZh() ? t(`print.name.${id}`) : name
}

/** 已登记的打印名称：中文 → 名称标识 */
export const printNameId = (name) => NAME_IDS[name]

/** 名称标识对应的中文名称（匹配键） */
export const printName = (id) => n[id]

const MATERIAL_COLUMNS = [n.index, n.materialCode, n.materialName, n.spec, n.unit]
const num = (title) => ({ title, align: 'right' })

const order = (key, name, partner, warehouse, confirm) => ({
  key,
  module: n.modScm,
  name,
  fields: [partner, n.orderDate, n.deliveryDate, warehouse, n.totalAmount, n.taxAmount, n.currencyRate, n.version],
  columns: [...MATERIAL_COLUMNS, num(n.quantity), num(n.taxPrice), num(n.taxRate), num(n.totalAmount), n.deliveryDate],
  signs: [n.prepared, n.approved, confirm],
  note: true
})
const trade = (key, name, partner, sign) => ({
  key,
  module: n.modScm,
  name,
  fields: [n.orderNo, partner, n.warehouse, n.bizDate, n.wmsDocNo, n.totalAmount, n.originalCurrency],
  columns: [...MATERIAL_COLUMNS, n.location, n.batch, num(n.quantity), num(n.taxPrice), num(n.totalAmount), n.serialNo],
  signs: [n.prepared, n.storekeeper, sign],
  note: true
})
const stock = (key, name, warehouse, transfer) => ({
  key,
  module: n.warehouse,
  name,
  fields: [warehouse, ...(transfer ? [n.inWarehouse] : []), n.bizDate, n.sourceNo, n.totalQty],
  columns: [...MATERIAL_COLUMNS, transfer ? n.outLocation : n.location, ...(transfer ? [n.inLocation] : []), n.batch, num(n.quantity), n.serialNo],
  signs: [n.prepared, n.confirmed, n.handler],
  note: true
})

/** 可配置打印模板的单据：名称、表头字段、明细列、签字栏与各页面打印时一致 */
export const PRINT_TYPES = [
  order('scm.purchaseOrder', n.purchaseOrder, n.supplier, n.receiveWarehouse, n.supplierConfirm),
  order('scm.salesOrder', n.salesOrder, n.customer, n.shipWarehouse, n.customerConfirm),
  trade('scm.purchaseIn', n.purchaseIn, n.supplier, n.supplierSign),
  trade('scm.purchaseReturn', n.purchaseReturn, n.supplier, n.supplierSign),
  trade('scm.salesOut', n.salesOut, n.customer, n.customerSign),
  trade('scm.salesReturn', n.salesReturn, n.customer, n.customerSign),
  {
    key: 'scm.pickList',
    module: n.modScm,
    name: n.pickList,
    fields: [n.salesOrder, n.customer, n.shipWarehouse, n.shipDate, n.prepared, n.status],
    columns: [n.index, n.material, n.location, n.batch, n.expiryDate, num(n.quantity), n.serialNo],
    signs: [n.prepared, n.picker, n.reviewer],
    note: true
  },
  {
    key: 'scm.wavePick',
    module: n.modScm,
    name: n.wavePick,
    fields: [n.warehouse, n.deliveryNotice, n.prepared, n.status],
    columns: [n.index, n.location, n.material, n.batch, n.expiryDate, num(n.quantity), n.sorting],
    signs: [n.prepared, n.picker, n.reviewer],
    note: true
  },
  {
    key: 'scm.packingList',
    module: n.modScm,
    name: n.packingList,
    fields: [n.salesOrder, n.customer, n.shipWarehouse, n.shipDate, n.boxCount, n.totalGrossWeight, n.totalVolume],
    columns: [n.boxNo, n.material, num(n.quantity), num(n.grossWeight), num(n.volume)],
    signs: [n.packer, n.reviewer, n.customerSign],
    note: true
  },
  stock('wms.in', n.wmsIn, n.inboundWarehouse),
  stock('wms.out', n.wmsOut, n.outboundWarehouse),
  stock('wms.transfer', n.transfer, n.outWarehouse, true),
  {
    key: 'fin.voucher',
    module: n.modFin,
    name: n.voucher,
    fields: [n.date, n.period, n.attachments],
    columns: [n.summary, n.account, n.deptAux, num(n.debit), num(n.credit)],
    signs: [n.prepared, n.approved, n.posted, n.finManager]
  },
  {
    key: 'fin.balanceSheet',
    module: n.modFin,
    name: n.balanceSheet,
    fields: [n.preparer, n.period, n.unit],
    columns: [n.assets, n.lineNo, num(n.endBalance), num(n.beginYearBalance), n.liabilitiesEquity, n.lineNoRight, num(n.endBalanceRight), num(n.beginYearBalanceRight)],
    signs: [n.entityHead, n.accountingManager, n.tabulator]
  },
  {
    key: 'fin.incomeStatement',
    module: n.modFin,
    name: n.incomeStatement,
    fields: [n.preparer, n.period, n.unit],
    columns: [n.item, n.lineNo, num(n.monthAmount), num(n.yearAmount), num(n.lastYearAmount)],
    signs: [n.entityHead, n.accountingManager, n.tabulator]
  },
  {
    key: 'fin.cashFlow',
    module: n.modFin,
    name: n.cashFlow,
    fields: [n.preparer, n.period, n.unit],
    columns: [n.item, n.lineNo, num(n.amount)],
    signs: [n.entityHead, n.accountingManager, n.tabulator]
  },
  {
    key: 'fin.partnerStatement',
    module: n.modFin,
    name: n.partnerStatement,
    fields: [n.customer, n.supplier, n.statementPeriod, n.beginBalance, n.endBalance],
    columns: [n.date, n.type, n.docNo, n.sourceSettle, num(n.increase), num(n.decrease), num(n.balance), n.remark],
    signs: [n.tabulator, n.finReview, n.customerSeal, n.supplierSeal],
    note: true
  },
  {
    key: 'mes.workOrder',
    module: n.modMes,
    name: n.workOrder,
    fields: [n.product, n.bomVersion, n.planQty, n.planStart, n.planFinish, n.finishWarehouse],
    columns: [...MATERIAL_COLUMNS, num(n.unitUsage), num(n.requiredQty), num(n.supplement), num(n.issued)],
    signs: [n.prepared, n.productionManager, n.warehouse],
    note: true
  },
  {
    key: 'hr.payslip',
    module: n.modHr,
    name: n.payslip,
    slip: true,
    fields: [n.empName, n.empNo, n.dept, n.attendance, n.baseSalary, n.allowance, n.overtimePay, n.pieceWage, n.performanceBonus, n.otherBonus,
      n.attendanceDeduction, n.grossPay, n.socialInsurance, n.housingFund, n.specialDeduction, n.incomeTax, n.otherDeduction, n.netPay, n.remark]
  }
].map((tp) => ({ ...tp, columns: (tp.columns || []).map((c) => (typeof c === 'string' ? { title: c } : c)), signs: tp.signs || [] }))

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
  { value: 'A4', label: t('print.paper.size', { name: 'A4', size: '210 × 297' }), width: 210, height: 297 },
  { value: 'A5', label: t('print.paper.size', { name: 'A5', size: '148 × 210' }), width: 148, height: 210 },
  { value: 'B5', label: t('print.paper.size', { name: 'B5', size: '176 × 250' }), width: 176, height: 250 },
  { value: '241x280', label: t('print.paper.size', { name: t('print.paper.continuous1'), size: '241 × 280' }), width: 241, height: 280 },
  { value: '241x140', label: t('print.paper.size', { name: t('print.paper.continuous2'), size: '241 × 140' }), width: 241, height: 140 },
  { value: '241x93', label: t('print.paper.size', { name: t('print.paper.continuous3'), size: '241 × 93' }), width: 241, height: 93 },
  { value: 'custom', label: t('print.paper.custom') }
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
  // 按中文名称判断数值项，示例为金额，其他为「示例 + 名称」
  const numeric = /数量|金额|单价|用量|已领|补料|税率/ // i18n-ignore 按中文匹配键判断
  const sample = (name, i) => (name === n.index ? String(i + 1) : numeric.test(name) ? '100.00' : t('print.sample.value', { name: printLabel(name) }))
  const item = (name, value) => ({ key: name, label: printLabel(name), value })
  return {
    title: printLabel(type.name),
    no: 'NO202601010001',
    slip: !!type.slip,
    fields: type.fields.map((f) => item(f, t('print.sample.value', { name: printLabel(f) }))),
    columns: type.columns.map((c) => ({ key: c.title, title: printLabel(c.title), right: c.align === 'right' })),
    rows: type.columns.length ? [0, 1, 2].map((i) => Object.fromEntries(type.columns.map((c) => [c.title, sample(c.title, i)]))) : [],
    note: type.note ? t('print.sample.note') : undefined,
    signs: type.signs.map((s) => item(s, t('print.sample.person')))
  }
}
