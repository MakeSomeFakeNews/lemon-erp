import { del, get, post } from '@/utils/request'
import { OPTION_PAGE_SIZE } from '@/api/wms'

/**
 * 手机开单：采购入库直接开单、出入库草稿（审核记账）、单据详情（打印）。
 * kind 为后端路径中的种类：salesOut / purchaseIn（另有 purchaseReturn / salesReturn，小程序暂不使用）
 */

/** 供应商选项 [{ id, code, name }]（登录即可访问）：按编码、名称、联系人、电话、拼音首字母、去掉分隔符的缩写分页搜索 */
export const supplierOptions = (keyword, current = 1) => get('/scm/supplier/options', { keyword, current, size: OPTION_PAGE_SIZE })

/**
 * 采购入库（直接开单）：{ partnerId（供应商）, warehouseId, bizDate, remark, invoiceKind,
 *   items: [{ materialId, qty, price, taxRate, unitName, locationId, batchNo, productionDate, expiryDate, serialNos }] }
 * qty、price 为基本单位（含税单价），unitName 为录入单位（仅显示）。返回新单据 ID
 */
export const addTradeDoc = (kind, data) => post(`/scm/${kind}`, data)
/** 保存为草稿（请求同 addTradeDoc），返回草稿 ID */
export const addTradeDraft = (kind, data) => post(`/scm/${kind}/drafts`, data)

/** 草稿列表：{ current, size, docNo? } → { records, total } */
export const pageTradeDraft = (kind, params) => get(`/scm/${kind}/drafts`, params)
/** 草稿详情（含明细 items） */
export const getTradeDraft = (kind, id) => get(`/scm/${kind}/drafts/${id}`)
/** 审核草稿：出入库、生成应收应付，返回生成的出入库单 ID */
export const postTradeDraft = (kind, id) => post(`/scm/${kind}/drafts/${id}/post`)
export const deleteTradeDraft = (kind, id) => del(`/scm/${kind}/drafts/${id}`)

/** 出入库单详情（含明细 items），打印用 */
export const getTradeDoc = (kind, id) => get(`/scm/${kind}/${id}`)

/** 打印模板配置：{ 模板标识: 配置 }（登录即可读取） */
export const printTemplates = () => get('/system/print-template', undefined, { silent: true })
