import { reactive } from 'vue'
import { message } from 'ant-design-vue'
import { printPdf } from '@/api/system/print'
import { useUserStore } from '@/store/user'
import { COMMON_KEY, applyTemplate, layoutOf, loadTemplates } from './printTemplates'

/**
 * 单据打印：页面组装单据，按「系统管理 - 打印模板」中的配置调整后发给后端，由 JasperReports 生成 PDF，
 * 在预览窗口（components/PrintPreview.vue）中打印或下载。
 * 后端按纸张自动分页：每页重复表头和列头，末页底部打印签字栏，页脚有页码；上传了 JRXML 的单据按 JRXML 打印。
 * 公司名称：打印模板通用设置 > SaaS 租户名称 > 环境变量 VITE_COMPANY_NAME
 */
export const COMPANY_NAME = import.meta.env.VITE_COMPANY_NAME || '柠檬ERP'

/** 打印预览窗口的状态（PrintPreview.vue 读取） */
export const printPreview = reactive({ open: false, url: '', title: '' })

/** 在预览窗口中显示一个 PDF（Blob） */
export function showPdf(blob, title) {
  if (printPreview.url) URL.revokeObjectURL(printPreview.url)
  printPreview.url = URL.createObjectURL(blob)
  printPreview.title = title
  printPreview.open = true
}

const text = (v) => (v === undefined || v === null ? null : String(v))

function toColumns(columns) {
  return (columns || []).map((c) => ({ key: c.title, title: c.title, right: c.align === 'right', ...(c.width ? { width: c.width } : {}) }))
}

function toRows(columns, rows) {
  return (rows || []).map((r, i) => Object.fromEntries((columns || []).map((c) => [c.title, text(c.value ? c.value(r, i) : r[c.key])])))
}

/**
 * 页面组装的单据转为打印请求的结构：表头字段、签字栏为 { key, label, value }，明细行为 { 列标题: 文本 }。
 * key 是单据中的原始名称（打印模板、上传的 JRXML 都按它匹配），label / title 是显示名称
 *
 * @param {object} doc
 * @param {string} doc.title   单据名称，如 采购订单
 * @param {string} [doc.no]    单号
 * @param {[string, any, boolean?][]} [doc.fields] 表头字段 [标签, 值, 是否整行]
 * @param {{title: string, key?: string, value?: Function, align?: 'right'}[]} [doc.columns] 明细列
 * @param {object[]} [doc.rows] 明细行
 * @param {object} [doc.totals] 合计行 { 列 key: 值 }，第一列没有值时显示「合计」
 * @param {(string | [string, any])[]} [doc.signs] 签字栏，如 ['审核', ['制单', '张三']]
 * @param {string} [doc.note]  备注
 * @param {{title: string, columns: object[], rows: object[]}[]} [doc.sections] 主表之后的其他明细表
 * @param {boolean} [doc.slip] 条状单据（工资条）：一页打印多条，之间用虚线分隔
 */
export function toPrintDoc(doc) {
  const columns = doc.columns || []
  const totals = doc.totals
    ? Object.fromEntries(columns.filter((c) => c.key && doc.totals[c.key] != null).map((c) => [c.title, text(doc.totals[c.key])]))
    : {}
  return {
    title: doc.title,
    no: text(doc.no),
    companyName: doc.companyName,
    note: text(doc.note),
    slip: !!doc.slip,
    fields: (doc.fields || []).map(([label, value, wide]) => ({ key: label, label, value: text(value), wide: !!wide })),
    columns: toColumns(columns),
    rows: toRows(columns, doc.rows),
    totals,
    signs: (doc.signs || []).map((s) => [].concat(s)).map(([label, value]) => ({ key: label, label, value: text(value) })),
    sections: (doc.sections || []).map((s) => ({ title: s.title, columns: toColumns(s.columns), rows: toRows(s.columns, s.rows) }))
  }
}

/** 默认公司名称：SaaS 模式下为当前租户名称 */
export function defaultCompanyName() {
  return useUserStore().tenantName || COMPANY_NAME
}

/**
 * 按模板生成打印请求（打印模板设计页预览也使用）
 *
 * @param list  规范化后的单据（toPrintDoc 的结果）
 * @param title 打印任务标题
 * @param type  打印模板标识
 * @param templates 已保存的模板
 * @param [tpl] 使用的模板配置，默认为 templates[type]
 */
export function buildPrintRequest(list, title, type, templates, tpl = type && templates[type]) {
  const docs = list.map((d) => applyTemplate({ ...d, companyName: d.companyName ?? defaultCompanyName() }, tpl, templates[COMMON_KEY]))
  // 套打：按定位输出（上传了 JRXML 时以 JRXML 为准）
  const overlay = tpl && !tpl.jrxml && tpl.overlay?.enabled ? tpl.overlay : undefined
  return { type, title, layout: layoutOf(tpl || {}), docs, overlay }
}

/**
 * 打印一张或多张单据（多张时每张单独分页、编页码）
 *
 * @param docs  单据（结构见 toPrintDoc）
 * @param title 打印任务标题（PDF 文件名）
 * @param type  打印模板标识（printTemplates.js 中的 PRINT_TYPES），不传则按默认版式
 */
export async function printDocs(docs, title = '打印', type) {
  const list = [].concat(docs).map(toPrintDoc)
  const templates = await loadTemplates().catch(() => ({}))
  const hide = message.loading({ content: '正在生成打印文件…', key: 'print', duration: 0 })
  try {
    showPdf(await printPdf(buildPrintRequest(list, title, type, templates)), title)
  } finally {
    hide()
  }
}

/** 在隐藏的 iframe 中打印一段完整的 HTML（条码标签） */
export function printHtml(html) {
  const iframe = document.createElement('iframe')
  iframe.style.cssText = 'position:fixed;right:0;bottom:0;width:0;height:0;border:0'
  document.body.appendChild(iframe)
  const w = iframe.contentWindow
  w.document.open()
  w.document.write(html)
  w.document.close()
  const cleanup = () => setTimeout(() => iframe.remove(), 500)
  w.onafterprint = cleanup
  setTimeout(() => {
    w.focus()
    w.print()
    // 部分浏览器不触发 afterprint
    setTimeout(cleanup, 60000)
  }, 100)
}

/** 物料明细的通用列：序号、编码、名称、规格、单位 */
export const materialColumns = () => [
  { title: '序号', value: (r, i) => i + 1 },
  { title: '物料编码', key: 'materialCode' },
  { title: '物料名称', key: 'materialName' },
  { title: '规格型号', key: 'spec' },
  { title: '单位', key: 'unit' }
]
