import request from '@/utils/request'

/** 采购入库 / 采购退货 / 销售出库 / 销售退货：kind = purchaseIn | purchaseReturn | salesOut | salesReturn */
export const pageTrade = (kind, params) => request.get(`/scm/${kind}`, { params })
export const getTrade = (kind, id) => request.get(`/scm/${kind}/${id}`)
/** 保存即确认，返回新单据 ID */
export const addTrade = (kind, data) => request.post(`/scm/${kind}`, data)
