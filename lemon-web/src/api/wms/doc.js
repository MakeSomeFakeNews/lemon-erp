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
export const receiveTransfer = (id, receiveDate) => request.post(`/wms/transfer/${id}/receive`, { receiveDate })
/**
 * 调拨价格快速计算：{ pricing（TransferPricing）, priceLevel?, factor?, materialIds: [id] } → { 物料ID: 调拨价 }（没有依据价的不返回）。
 * 需要「查看成本金额」权限；同价调拨按当前库存均价预览
 */
export const transferPrices = ({ materialIds, ...params }) =>
  request.get('/wms/transfer/prices', { params: { ...params, materialIds: materialIds.join(',') } })
/** 调拨汇总：{ warehouseId?, beginDate?, endDate? } → 按调出仓库 → 调入仓库汇总的行 */
export const transferSummary = (params) => request.get('/wms/transfer/summary', { params })
