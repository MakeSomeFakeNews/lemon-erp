/**
 * Excel 导入导出（exceljs 按需加载，不进入首屏包）
 */
import { t } from '@/locales'

const loadExcel = () => import('exceljs').then((m) => m.default || m)

/** 浏览器下载 Blob */
export function saveBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

const stamp = () => {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}${p(d.getMonth() + 1)}${p(d.getDate())}${p(d.getHours())}${p(d.getMinutes())}`
}

/**
 * 导出 xlsx
 * @param {string} name 文件名（不含扩展名，自动加时间）
 * @param {{title: string, width?: number}[]} headers 表头
 * @param {any[][]} rows 数据行
 */
export async function writeExcel(name, headers, rows, { withStamp = true } = {}) {
  const ExcelJS = await loadExcel()
  const wb = new ExcelJS.Workbook()
  const ws = wb.addWorksheet(name.slice(0, 31) || 'Sheet1')
  ws.columns = headers.map((h) => ({ header: h.title, width: h.width || Math.max(10, String(h.title).length * 2 + 4) }))
  ws.getRow(1).font = { bold: true }
  ws.views = [{ state: 'frozen', ySplit: 1 }]
  rows.forEach((r) => ws.addRow(r))
  const buf = await wb.xlsx.writeBuffer()
  saveBlob(
    new Blob([buf], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }),
    `${name}${withStamp ? '_' + stamp() : ''}.xlsx`
  )
}

const cellText = (v) => {
  if (v === null || v === undefined) return undefined
  if (v instanceof Date) {
    const p = (n) => String(n).padStart(2, '0')
    return `${v.getUTCFullYear()}-${p(v.getUTCMonth() + 1)}-${p(v.getUTCDate())}`
  }
  if (typeof v === 'object') {
    if ('result' in v) return cellText(v.result)
    if ('text' in v) return String(v.text).trim()
    if (Array.isArray(v.richText)) return v.richText.map((t) => t.text).join('').trim()
    return undefined
  }
  const s = String(v).trim()
  return s === '' ? undefined : s
}

/**
 * 读取 xlsx 第一个工作表：第一行为表头，按表头名称对应字段
 * @param {File} file
 * @param {{title: string, key: string}[]} columns
 * @param {Record<string, string>} [headerMap] 字段 → Excel 表头（表头与模板不一致时按此对应，见 AI 对应字段；不在其中的字段不读取）
 * @returns {Promise<{rowNum: number, [key: string]: string}[]>} 值均为去掉首尾空格的字符串，rowNum 为 Excel 行号
 */
export async function readExcel(file, columns, headerMap) {
  const ExcelJS = await loadExcel()
  const wb = new ExcelJS.Workbook()
  await wb.xlsx.load(await file.arrayBuffer())
  const ws = wb.worksheets[0]
  if (!ws) throw new Error(t('components.excel.noSheet'))
  const header = {}
  ws.getRow(1).eachCell((cell, col) => {
    const txt = cellText(cell.value)
    // 表头可能带「*」表示必填
    if (txt) header[txt.replace(/^\*/, '').trim()] = col
  })
  const titleOf = (c) => (headerMap ? headerMap[c.key] : c.title)
  const missing = columns.filter((c) => c.required && !header[titleOf(c)]).map((c) => c.title)
  if (missing.length) throw new Error(t('components.excel.missingColumns', { columns: missing.join(t('components.excel.listSep')) }))
  const rows = []
  ws.eachRow((row, rowNum) => {
    if (rowNum === 1) return
    const item = { rowNum }
    let empty = true
    columns.forEach((c) => {
      const col = titleOf(c) ? header[titleOf(c)] : undefined
      const v = col ? cellText(row.getCell(col).value) : undefined
      if (v !== undefined) {
        item[c.key] = v
        empty = false
      }
    })
    if (!empty) rows.push(item)
  })
  return rows
}

/**
 * 读取 xlsx 第一个工作表的表头（去掉表示必填的「*」）和前几行数据（按列顺序的文字），用于表头与模板不一致时对应字段
 * @returns {Promise<{headers: string[], samples: string[][]}>}
 */
export async function readExcelHeaders(file, sampleRows = 3) {
  const ExcelJS = await loadExcel()
  const wb = new ExcelJS.Workbook()
  await wb.xlsx.load(await file.arrayBuffer())
  const ws = wb.worksheets[0]
  if (!ws) throw new Error(t('components.excel.noSheet'))
  const cols = []
  const headers = []
  ws.getRow(1).eachCell((cell, col) => {
    const txt = cellText(cell.value)
    if (txt) {
      cols.push(col)
      headers.push(txt.replace(/^\*/, '').trim())
    }
  })
  const samples = []
  for (let r = 2; r <= ws.rowCount && samples.length < sampleRows; r++) {
    const row = ws.getRow(r)
    const values = cols.map((col) => cellText(row.getCell(col).value) ?? '')
    if (values.some((v) => v !== '')) samples.push(values)
  }
  return { headers, samples }
}

/** 下载导入模板：必填列表头加「*」，可附一行示例 */
export function downloadTemplate(name, columns, sample) {
  return writeExcel(
    t('components.excel.templateName', { name }),
    columns.map((c) => ({ title: (c.required ? '*' : '') + c.title, width: c.width })),
    sample ? [columns.map((c) => sample[c.key] ?? '')] : [],
    { withStamp: false }
  )
}
