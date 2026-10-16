import request from '@/utils/request'

/** 库存冻结（待检、不合格）：{ status, keyword, sourceNo } */
export const pageHold = (params) => request.get('/wms/hold', { params })
/** 放行（让步接收）：remark 说明 */
export const releaseHold = (id, remark) => request.post(`/wms/hold/${id}/release`, { remark })
