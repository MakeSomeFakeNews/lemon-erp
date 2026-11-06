/**
 * 智能录入的输入处理：Excel 在浏览器中解析成表格文字后提交（exceljs 按需加载），文件类型和大小的前端检查
 * （与后端 AiDocumentReader 的限制一致，后端仍会校验）
 */
const loadExcel = () => import('exceljs').then((m) => m.default || m)

export const MAX_IMAGES = 5
export const MAX_FILES = 6
const MB = 1024 * 1024
const LIMITS = { image: 5 * MB, pdf: 10 * MB, text: 2 * MB, sheet: 10 * MB }
/** Excel 最多读取的工作表数、每个工作表的行数（足够一张单据，避免把整本台账发给模型） */
const MAX_SHEETS = 3
const MAX_ROWS = 300

export const ACCEPT = 'image/png,image/jpeg,image/webp,.pdf,.xlsx,.csv,.txt'

/** 文件种类：image / pdf / sheet（Excel，前端解析）/ text（csv、txt，后端解码）；不支持时为 null */
export function fileKind(file) {
  const name = (file.name || '').toLowerCase()
  if (['image/png', 'image/jpeg', 'image/webp'].includes(file.type) || /\.(png|jpe?g|webp)$/.test(name)) return 'image'
  if (file.type === 'application/pdf' || name.endsWith('.pdf')) return 'pdf'
  if (name.endsWith('.xlsx')) return 'sheet'
  if (/\.(csv|txt|tsv)$/.test(name)) return 'text'
  return null
}

/** 超过大小限制时返回上限（MB），否则 null */
export function overLimit(file, kind) {
  return file.size > LIMITS[kind] ? LIMITS[kind] / MB : null
}

const cellText = (v) => {
  if (v === null || v === undefined) return ''
  if (v instanceof Date) {
    const p = (n) => String(n).padStart(2, '0')
    return `${v.getUTCFullYear()}-${p(v.getUTCMonth() + 1)}-${p(v.getUTCDate())}`
  }
  if (typeof v === 'object') {
    if ('result' in v) return cellText(v.result)
    if ('text' in v) return String(v.text).trim()
    if (Array.isArray(v.richText)) return v.richText.map((r) => r.text).join('').trim()
    return ''
  }
  return String(v).replace(/[\t\r\n]+/g, ' ').trim()
}

/** xlsx 解析成制表符分隔的文字（每个工作表以「# 工作表名」开头，跳过空行） */
export async function sheetToText(file) {
  const ExcelJS = await loadExcel()
  const wb = new ExcelJS.Workbook()
  await wb.xlsx.load(await file.arrayBuffer())
  const parts = []
  wb.worksheets.slice(0, MAX_SHEETS).forEach((ws) => {
    const lines = []
    ws.eachRow((row) => {
      if (lines.length >= MAX_ROWS) return
      const cells = []
      row.eachCell({ includeEmpty: true }, (cell) => cells.push(cellText(cell.value)))
      while (cells.length && !cells[cells.length - 1]) cells.pop()
      if (cells.some(Boolean)) lines.push(cells.join('\t'))
    })
    if (lines.length) parts.push(`# ${ws.name}\n${lines.join('\n')}`)
  })
  return parts.join('\n\n')
}
