import request from '@/utils/request'

/**
 * 打印模板。同一单据可以有多份打印格式：按模板标识的接口带 formatId 时操作该格式，不带时操作默认格式（标签、通用设置只有默认格式）
 */
const withFormat = (formatId) => (formatId ? { params: { formatId } } : {})

/** 已修改的打印模板（各单据的默认格式）：{ 模板标识: 配置 }，上传了 JRXML 的带 jrxml: true */
export const listPrintTemplates = () => request.get('/system/print-template')
export const savePrintTemplate = (key, config, formatId) => request.put(`/system/print-template/${key}`, config, withFormat(formatId))
/** 恢复默认版式（同时删除上传的 JRXML）：带 formatId 时只清空该格式，不带时删除该单据的全部格式 */
export const resetPrintTemplate = (key, formatId) => request.delete(`/system/print-template/${key}`, withFormat(formatId))

/** 各单据的打印格式：[{ id, templateKey, name, isDefault, sort, status, jrxml, config }] */
export const listPrintFormats = () => request.get('/system/print-template/formats')
/** 新增打印格式：{ templateKey, name, sourceId（复制来源）, isDefault, sort, status } */
export const createPrintFormat = (data) => request.post('/system/print-template/formats', data)
/** 修改名称、排序、状态，isDefault: true 设为默认 */
export const updatePrintFormat = (id, data) => request.put(`/system/print-template/formats/${id}`, data)
export const deletePrintFormat = (id) => request.delete(`/system/print-template/formats/${id}`)

/** 上传的 JRXML（Blob） */
export const downloadJrxml = (key, formatId) =>
  request.get(`/system/print-template/${key}/jrxml`, { responseType: 'blob', ...withFormat(formatId) })
export const uploadJrxml = (key, file, formatId) => {
  const data = new FormData()
  data.append('file', file)
  return request.post(`/system/print-template/${key}/jrxml`, data, { timeout: 60000, ...withFormat(formatId) })
}
export const removeJrxml = (key, formatId) => request.delete(`/system/print-template/${key}/jrxml`, withFormat(formatId))

/** 套打底图（data URL），保存为空时删除 */
export const getPrintBackground = (key, formatId) => request.get(`/system/print-template/${key}/background`, withFormat(formatId))
export const savePrintBackground = (key, dataUrl, formatId) =>
  request.put(`/system/print-template/${key}/background`, { dataUrl }, withFormat(formatId))
