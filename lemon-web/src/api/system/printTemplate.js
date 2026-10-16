import request from '@/utils/request'

/** 已修改的打印模板：{ 模板标识: 配置 }，上传了 JRXML 的带 jrxml: true */
export const listPrintTemplates = () => request.get('/system/print-template')
export const savePrintTemplate = (key, config) => request.put(`/system/print-template/${key}`, config)
/** 恢复默认版式（同时删除上传的 JRXML） */
export const resetPrintTemplate = (key) => request.delete(`/system/print-template/${key}`)

/** 上传的 JRXML（Blob） */
export const downloadJrxml = (key) => request.get(`/system/print-template/${key}/jrxml`, { responseType: 'blob' })
export const uploadJrxml = (key, file) => {
  const data = new FormData()
  data.append('file', file)
  return request.post(`/system/print-template/${key}/jrxml`, data, { timeout: 60000 })
}
export const removeJrxml = (key) => request.delete(`/system/print-template/${key}/jrxml`)
