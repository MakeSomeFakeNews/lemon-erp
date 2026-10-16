import request from '@/utils/request'

/**
 * 出入库单接口，kind：in 入库 / out 出库 / transfer 调拨
 */
export const pageDoc = (kind, params) => request.get(`/wms/${kind}`, { params })
export const getDoc = (kind, id) => request.get(`/wms/${kind}/${id}`)
/** 返回新单据 ID */
export const addDoc = (kind, data) => request.post(`/wms/${kind}`, data)
export const updateDoc = (kind, id, data) => request.put(`/wms/${kind}/${id}`, data)
export const confirmDoc = (kind, id) => request.post(`/wms/${kind}/${id}/confirm`)
export const deleteDoc = (kind, ids) => request.delete(`/wms/${kind}/${[].concat(ids).join(',')}`)
/** 冲销已完成的单据，返回红字单据 ID */
export const reverseDoc = (kind, id) => request.post(`/wms/${kind}/${id}/reverse`)
/** 在途调拨单收货：调入仓库入库 */
export const receiveTransfer = (id) => request.post(`/wms/transfer/${id}/receive`)
