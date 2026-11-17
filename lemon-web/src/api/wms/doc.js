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
/** 在途调拨的验收单、差异、补发单和各明细未到的序列号：{ receipts, diffs, supplements, availableSerials } */
export const getTransferTransit = (id) => request.get(`/wms/transfer/${id}/transit`)
/**
 * 在途调拨的调拨入库（验收）：{ receiveDate, logisticsNo, plateNo, remark, lines: [{ itemId, qty, serialNos, diffType, diffQty, diffSerialNos, diffRemark }] }，
 * 不传 lines 时全部未到量验收入库；返回验收单 ID
 */
export const receiveTransfer = (id, data) => request.post(`/wms/transfer/${id}/receive`, data)
/** 处理差异：{ action（TransferResolution）, qty?, serialNos?, bizDate?, remark? }，返回生成的单据 ID（报废出库单 / 调拨单草稿） */
export const resolveTransferDiff = (diffId, data) => request.post(`/wms/transfer/diff/${diffId}/resolve`, data)
/** 处理未到量：{ action（RETURN / LOSS）, bizDate?, remark? }，返回确认损耗生成的报废出库单 ID */
export const closeTransfer = (id, data) => request.post(`/wms/transfer/${id}/close`, data)
/**
 * 调拨价格快速计算：{ pricing（TransferPricing）, priceLevel?, factor?, materialIds: [id] } → { 物料ID: 调拨价 }（没有依据价的不返回）。
 * 需要「查看成本金额」权限；同价调拨按当前库存均价预览
 */
export const transferPrices = ({ materialIds, ...params }) =>
  request.get('/wms/transfer/prices', { params: { ...params, materialIds: materialIds.join(',') } })
/** 调拨汇总：{ warehouseId?, beginDate?, endDate? } → 按调出仓库 → 调入仓库汇总的行 */
export const transferSummary = (params) => request.get('/wms/transfer/summary', { params })
