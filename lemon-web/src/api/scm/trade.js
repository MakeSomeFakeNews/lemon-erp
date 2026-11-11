import request from '@/utils/request'

/** 采购入库 / 采购退货 / 销售出库 / 销售退货：kind = purchaseIn | purchaseReturn | salesOut | salesReturn */
export const pageTrade = (kind, params) => request.get(`/scm/${kind}`, { params })
export const getTrade = (kind, id) => request.get(`/scm/${kind}/${id}`)
/** 保存即审核（出入库），返回新单据 ID；业务参数 scm.tradeDoc.draft 开启时需要审核权限 */
export const addTrade = (kind, data) => request.post(`/scm/${kind}`, data)

/** 草稿（业务参数 scm.tradeDoc.draft）：不影响库存、订单、应收应付，审核后生成出入库单并删除草稿 */
export const pageTradeDraft = (kind, params) => request.get(`/scm/${kind}/drafts`, { params })
export const getTradeDraft = (kind, id) => request.get(`/scm/${kind}/drafts/${id}`)
/** 返回草稿 ID */
export const addTradeDraft = (kind, data) => request.post(`/scm/${kind}/drafts`, data)
export const updateTradeDraft = (kind, id, data) => request.put(`/scm/${kind}/drafts/${id}`, data)
export const delTradeDraft = (kind, ids) => request.delete(`/scm/${kind}/drafts/${ids}`)
/** 审核：data 不为空时先按其修改草稿；返回生成的出入库单 ID */
export const postTradeDraft = (kind, id, data) => request.post(`/scm/${kind}/drafts/${id}/post`, data)
