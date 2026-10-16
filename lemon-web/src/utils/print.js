import { useUserStore } from '@/store/user'
import { COMMON_KEY, applyTemplate, loadTemplates } from './printTemplates'

/**
 * 单据打印：生成 A4 打印页面放入隐藏 iframe 后调用浏览器打印（可另存为 PDF）。
 * 传入模板标识时按「系统管理 - 打印模板」中的配置调整版式（见 printTemplates.js）。
 * 公司名称：打印模板通用设置 > SaaS 租户名称 > 环境变量 VITE_COMPANY_NAME
 */
export const COMPANY_NAME = import.meta.env.VITE_COMPANY_NAME || '柠檬ERP'

const esc = (v) =>
  String(v ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')

const style = (orientation) => `
  @page { size: A4 ${orientation === 'landscape' ? 'landscape' : 'portrait'}; margin: 12mm; }
  * { box-sizing: border-box; }
  body { font-family: "PingFang SC", "Microsoft YaHei", sans-serif; font-size: 12px; color: #000; margin: 0; }
  .doc { page-break-after: always; }
  .doc:last-child { page-break-after: auto; }
  .company { text-align: center; font-size: 13px; }
  h1 { text-align: center; font-size: 20px; margin: 4px 0 2px; letter-spacing: 4px; }
  .sub { text-align: center; margin-bottom: 10px; }
  .fields { display: grid; grid-template-columns: repeat(3, 1fr); gap: 4px 12px; margin-bottom: 8px; }
  .fields .wide { grid-column: span 3; }
  table { width: 100%; border-collapse: collapse; }
  th, td { border: 1px solid #000; padding: 4px 6px; }
  th { background: #f2f2f2; font-weight: 600; }
  td.r { text-align: right; }
  tfoot td { font-weight: 600; }
  .signs { display: flex; justify-content: space-between; margin-top: 18px; }
  .signs span { min-width: 120px; }
  .note { margin-top: 8px; }
  .section { margin: 10px 0 4px; font-weight: 600; }
  .doc.slip { page-break-after: auto; page-break-inside: avoid; border-bottom: 1px dashed #888; padding-bottom: 10px; margin-bottom: 12px; }
  .doc.slip h1 { font-size: 15px; letter-spacing: 1px; }
`

function renderTable(columns, rows, totals) {
  const cols = columns || []
  if (!cols.length) return ''
  const cls = (c) => (c.align === 'right' ? 'r' : '')
  const cell = (c, r, i) => esc(c.value ? c.value(r, i) : r[c.key])
  const width = (c) => (c.width ? ` style="width:${Number(c.width)}mm"` : '')
  const head = `<thead><tr>${cols.map((c) => `<th${width(c)}>${esc(c.title)}</th>`).join('')}</tr></thead>`
  const body = `<tbody>${(rows || [])
    .map((r, i) => `<tr>${cols.map((c) => `<td class="${cls(c)}">${cell(c, r, i)}</td>`).join('')}</tr>`)
    .join('')}</tbody>`
  const foot = totals
    ? `<tfoot><tr>${cols.map((c, i) => `<td class="${cls(c)}">${i === 0 ? '合计' : esc(totals[c.key] ?? '')}</td>`).join('')}</tr></tfoot>`
    : ''
  return `<table>${head}${body}${foot}</table>`
}

/**
 * @param {object} doc
 * @param {string} doc.title   单据名称，如 采购订单
 * @param {string} [doc.no]    单号
 * @param {[string, any, boolean?][]} [doc.fields] 表头字段 [标签, 值, 是否整行]
 * @param {{title: string, key?: string, value?: Function, align?: 'right'}[]} [doc.columns] 明细列
 * @param {object[]} [doc.rows] 明细行
 * @param {object} [doc.totals] 合计行 { 列 key: 值 }，第一列显示「合计」
 * @param {(string | [string, any])[]} [doc.signs] 签字栏，如 ['审核', ['制单', '张三']]
 * @param {string} [doc.note]  备注
 * @param {{title: string, columns: object[], rows: object[]}[]} [doc.sections] 主表之后的其他明细表
 * @param {boolean} [doc.slip] 条状单据（工资条）：不单独占一页，之间用虚线分隔
 */
function renderDoc(doc) {
  const fields = (doc.fields || [])
    .map(([label, value, wide]) => `<div class="${wide ? 'wide' : ''}">${esc(label)}：${esc(value ?? '-')}</div>`)
    .join('')
  const table = renderTable(doc.columns, doc.rows, doc.totals)
  const sections = (doc.sections || [])
    .map((sec) => `<div class="section">${esc(sec.title)}</div>${renderTable(sec.columns, sec.rows)}`)
    .join('')
  const signs = doc.signs?.length
    ? `<div class="signs">${doc.signs
        .map((s) => [].concat(s))
        .map(([label, value]) => `<span>${esc(label)}：${esc(value ?? '')}</span>`)
        .join('')}</div>`
    : ''
  return `<div class="doc${doc.slip ? ' slip' : ''}">
    ${doc.slip || !doc.companyName ? '' : `<div class="company">${esc(doc.companyName)}</div>`}
    <h1>${esc(doc.title)}</h1>
    ${doc.no ? `<div class="sub">No. ${esc(doc.no)}</div>` : ''}
    <div class="fields">${fields}</div>
    ${table}
    ${sections}
    ${doc.note ? `<div class="note">备注：${esc(doc.note)}</div>` : ''}
    ${signs}
  </div>`
}

/** 默认公司名称：SaaS 模式下为当前租户名称 */
export function defaultCompanyName() {
  return useUserStore().tenantName || COMPANY_NAME
}

/**
 * 生成打印页面的 HTML（单据已套用模板），打印模板设计页预览也使用
 */
export function renderHtml(list, title = '打印') {
  // 模板中关闭公司名称时为空字符串，未设置时用默认名称
  const docs = list.map((d) => ({ ...d, companyName: d.companyName ?? defaultCompanyName() }))
  return (
    `<!doctype html><html><head><meta charset="utf-8"><title>${esc(title)}</title>` +
    `<style>${style(docs[0]?.orientation)}</style></head><body>${docs.map(renderDoc).join('')}</body></html>`
  )
}

/**
 * 打印一张或多张单据（多张时每张一页）
 *
 * @param docs  单据（结构见 renderDoc）
 * @param title 打印任务标题（另存为 PDF 时的默认文件名）
 * @param type  打印模板标识（printTemplates.js 中的 PRINT_TYPES），不传则按默认版式
 */
export async function printDocs(docs, title = '打印', type) {
  let list = [].concat(docs)
  try {
    const templates = await loadTemplates()
    list = list.map((d) => applyTemplate(d, type && templates[type], templates[COMMON_KEY]))
  } catch {
    // 模板加载失败时按默认版式打印
  }
  printHtml(renderHtml(list, title))
}

/** 在隐藏的 iframe 中打印一段完整的 HTML（单据、标签共用） */
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
