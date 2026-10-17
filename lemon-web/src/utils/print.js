import { reactive } from 'vue'
import { message } from 'ant-design-vue'
import { printPdf } from '@/api/system/print'
import { DEFAULT_LOCALE, currentLocale, i18n, isZh, t } from '@/locales'
import { useUserStore } from '@/store/user'
import { COMMON_KEY, PRINT_TYPE_MAP, applyTemplate, layoutOf, loadTemplates, printLabel, printName } from './printTemplates'

/**
 * 单据打印：页面组装单据，按「系统管理 - 打印模板」中的配置调整后发给后端，由 JasperReports 生成 PDF，
 * 在预览窗口（components/PrintPreview.vue）中打印或下载。
 * 后端按纸张自动分页：每页重复表头和列头，末页底部打印签字栏，页脚有页码；上传了 JRXML 的单据按 JRXML 打印。
 * 公司名称：打印模板通用设置 > SaaS 租户名称 > 环境变量 VITE_COMPANY_NAME
 *
 * 多语言：表头字段、明细列、签字栏的匹配键始终是中文名称（printTemplates.js 的 PRINT_TYPES 中登记的名称），显示名称按界面语言：
 * - 调用处可以直接写中文名称（显示时自动翻译），也可以传入 t() 翻译后的名称（按语言包反查回中文匹配键）
 * - 也可以显式指定匹配键：字段 [显示名称, 值, 是否整行, 匹配键]，签字栏 [显示名称, 值, 匹配键]，明细列 { title, id: 匹配键 }
 */
export const COMPANY_NAME = import.meta.env.VITE_COMPANY_NAME || t('print.common.defaultCompany')

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

// ---------------------------------------------------------------- 匹配键（中文名称）

/** 语言包反查：译文 → 中文（同一译文可能对应多个中文），带参数的译文按模板反推 */
let reverse = null
function reverseIndex() {
  if (reverse) return reverse
  const exact = new Map()
  const patterns = []
  const literal = (s) => s.replace(/\{'(.)'\}/g, '$1')
  const walk = (tr, zh) => {
    for (const [k, v] of Object.entries(tr || {})) {
      const z = zh?.[k]
      if (v && typeof v === 'object') walk(v, z)
      else if (typeof v === 'string' && typeof z === 'string') {
        const names = [...v.matchAll(/\{(\w+)\}/g)].map((m) => m[1])
        if (!names.length) {
          const key = literal(v)
          if (!exact.has(key)) exact.set(key, new Set())
          exact.get(key).add(literal(z))
        } else {
          const source = literal(v)
            .split(/\{\w+\}/)
            .map((part) => part.replace(/[.*+?^$()|[\]\\]/g, '\\$&'))
            .join('(.+?)')
          patterns.push({ re: new RegExp(`^${source}$`), names, zh: literal(z) })
        }
      }
    }
  }
  walk(i18n.global.getLocaleMessage(currentLocale()), i18n.global.getLocaleMessage(DEFAULT_LOCALE))
  reverse = { exact, patterns }
  return reverse
}

/**
 * 显示名称对应的中文匹配键：中文界面或已是候选名称时原样返回；其他语言下先按打印名称的译文、再按语言包反查，
 * 只接受该单据登记过的名称（candidates），查不到时原样返回
 */
function resolveKey(label, candidates) {
  if (!label || isZh() || !candidates?.length || candidates.includes(label)) return label
  const byLabel = candidates.find((c) => printLabel(c) === label)
  if (byLabel) return byLabel
  const set = new Set(candidates)
  const { exact, patterns } = reverseIndex()
  for (const zh of exact.get(label) || []) if (set.has(zh)) return zh
  for (const p of patterns) {
    const m = label.match(p.re)
    if (!m) continue
    // 每个参数的候选中文（参数本身可能也是译文），组合后与登记的名称比对
    let fills = [{}]
    p.names.forEach((name, i) => {
      const options = [...(exact.get(m[i + 1]) || []), m[i + 1]]
      fills = fills.flatMap((f) => options.map((o) => ({ ...f, [name]: o }))).slice(0, 50)
    })
    for (const f of fills) {
      const zh = p.zh.replace(/\{(\w+)\}/g, (_, name) => f[name] ?? '')
      if (set.has(zh)) return zh
    }
  }
  return label
}

/** 条目的显示名称：传入的是中文匹配键时按界面语言翻译 */
const displayOf = (label, key) => (label === key ? printLabel(label) : label)

/** 列的匹配标识：显式的 id，否则为标题对应的中文名称；同一张单据中有同名列（如资产负债表左右两栏的行次）时用 id 区分 */
function withColumnIds(columns, candidates) {
  return (columns || []).map((c) => ({ ...c, matchId: c.id || resolveKey(c.title, candidates) }))
}

function toColumns(columns) {
  return columns.map((c) => ({
    key: c.matchId,
    title: c.id ? printLabel(c.title) : displayOf(c.title, c.matchId),
    right: c.align === 'right',
    ...(c.width ? { width: c.width } : {})
  }))
}

function toRows(columns, rows) {
  return (rows || []).map((r, i) => Object.fromEntries(columns.map((c) => [c.matchId, text(c.value ? c.value(r, i) : r[c.key])])))
}

/**
 * 页面组装的单据转为打印请求的结构：表头字段、签字栏为 { key, label, value }，明细行为 { 列匹配键: 文本 }。
 * key 是中文名称（打印模板、上传的 JRXML 都按它匹配），label / title 是当前语言的显示名称
 *
 * @param {object} doc
 * @param {string} doc.title   单据名称，如 采购订单
 * @param {string} [doc.no]    单号
 * @param {[string, any, boolean?, string?][]} [doc.fields] 表头字段 [标签, 值, 是否整行, 匹配键]
 * @param {{title: string, id?: string, key?: string, value?: Function, align?: 'right', width?: number}[]} [doc.columns] 明细列（id 为打印模板匹配用的标识，默认同标题；width 单位 mm）
 * @param {object[]} [doc.rows] 明细行
 * @param {object} [doc.totals] 合计行 { 列 key: 值 }，第一列没有值时显示「合计」
 * @param {(string | [string, any, string?])[]} [doc.signs] 签字栏，如 ['审核', ['制单', '张三']]，第三项为匹配键
 * @param {string} [doc.note]  备注
 * @param {{title: string, columns: object[], rows: object[]}[]} [doc.sections] 主表之后的其他明细表
 * @param {boolean} [doc.slip] 条状单据（工资条）：一页打印多条，之间用虚线分隔
 * @param {string} [type] 打印模板标识，用于把翻译后的名称对应回中文匹配键
 */
export function toPrintDoc(doc, type) {
  const def = PRINT_TYPE_MAP[type]
  const columnNames = def?.columns.map((c) => c.title) || []
  const columns = withColumnIds(doc.columns, columnNames)
  const totals = doc.totals
    ? Object.fromEntries(columns.filter((c) => c.key && doc.totals[c.key] != null).map((c) => [c.matchId, text(doc.totals[c.key])]))
    : {}
  const item = (label, value, key, candidates) => {
    const k = key || resolveKey(label, candidates)
    return { key: k, label: key ? printLabel(label) : displayOf(label, k), value: text(value) }
  }
  return {
    title: printLabel(doc.title),
    no: text(doc.no),
    companyName: doc.companyName,
    note: text(doc.note),
    slip: !!doc.slip,
    fields: (doc.fields || []).map(([label, value, wide, key]) => ({ ...item(label, value, key, def?.fields), wide: !!wide })),
    columns: toColumns(columns),
    rows: toRows(columns, doc.rows),
    totals,
    signs: (doc.signs || []).map((s) => [].concat(s)).map(([label, value, key]) => item(label, value, key, def?.signs)),
    sections: (doc.sections || []).map((s) => {
      const cols = withColumnIds(s.columns, [])
      return { title: printLabel(s.title), columns: toColumns(cols), rows: toRows(cols, s.rows) }
    })
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
export async function printDocs(docs, title = t('print.common.print'), type) {
  const list = [].concat(docs).map((d) => toPrintDoc(d, type))
  const templates = await loadTemplates().catch(() => ({}))
  const hide = message.loading({ content: t('print.common.generating'), key: 'print', duration: 0 })
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

/** 物料明细的通用列：序号、编码、名称、规格、单位（显示名称按界面语言，匹配 id 为中文名称） */
const materialColumn = (id, extra) => ({ title: t(`print.name.${id}`), id: printName(id), ...extra })
export const materialColumns = () => [
  materialColumn('index', { value: (r, i) => i + 1 }),
  materialColumn('materialCode', { key: 'materialCode' }),
  materialColumn('materialName', { key: 'materialName' }),
  materialColumn('spec', { key: 'spec' }),
  materialColumn('unit', { key: 'unit' })
]
