import request from '@/utils/request'

/** 生成 PDF（Blob）：{ type, title, layout, docs }，结构见 utils/print.js 的 toPrintDoc */
export const printPdf = (data) => request.post('/system/print/pdf', data, { responseType: 'blob', timeout: 120000 })

/** 当前配置下的默认版式 JRXML（Blob） */
export const defaultJrxml = (data) => request.post('/system/print/jrxml', data, { responseType: 'blob', timeout: 60000 })
