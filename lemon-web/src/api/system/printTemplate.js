import request from '@/utils/request'

/** 已修改的打印模板：{ 模板标识: 配置 } */
export const listPrintTemplates = () => request.get('/system/print-template')
export const savePrintTemplate = (key, config) => request.put(`/system/print-template/${key}`, config)
export const resetPrintTemplate = (key) => request.delete(`/system/print-template/${key}`)
