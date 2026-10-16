import request from '@/utils/request'

export const pageShipment = (params) => request.get('/scm/shipment', { params })
export const getShipment = (id) => request.get(`/scm/shipment/${id}`)
export const addShipment = (data) => request.post('/scm/shipment', data)
export const updateShipment = (id, data) => request.put(`/scm/shipment/${id}`, data)
/** action：ship 发货 / track 物流动态 / exception 异常 / sign 签收；data：{ date, trackingNo, content } */
export const shipmentAction = (id, action, data) => request.post(`/scm/shipment/${id}/${action}`, data)
export const deleteShipment = (ids) => request.delete(`/scm/shipment/${[].concat(ids).join(',')}`)

/** 波次拣货 */
export const pageWave = (params) => request.get('/scm/wave', { params })
export const getWave = (id) => request.get(`/scm/wave/${id}`)
/** { lines: [{ locationCode, zone, materialCode, materialName, unit, batchNo, expiryDate, qty, sorts: [{ deliveryNo, customerName, qty }] }], shortages } */
export const wavePickList = (id) => request.get(`/scm/wave/${id}/pick-list`)
/** data：{ deliveryIds, remark }，返回波次 ID */
export const addWave = (data) => request.post('/scm/wave', data)
export const addWaveDeliveries = (id, deliveryIds) => request.post(`/scm/wave/${id}/deliveries`, { deliveryIds })
export const removeWaveDelivery = (id, deliveryId) => request.delete(`/scm/wave/${id}/deliveries/${deliveryId}`)
export const resuggestWave = (id) => request.post(`/scm/wave/${id}/resuggest`)
export const cancelWave = (id) => request.post(`/scm/wave/${id}/cancel`)
/** 完成拣货，返回确认拣货的发货通知数 */
export const completeWave = (id) => request.post(`/scm/wave/${id}/complete`)
