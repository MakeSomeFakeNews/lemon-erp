import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

export const pageLocation = (params) => request.get('/wms/location', { params })
export const getLocation = (id) => request.get(`/wms/location/${id}`)
export const addLocation = (data) => request.post('/wms/location', data)
export const updateLocation = (id, data) => request.put(`/wms/location/${id}`, data)
export const changeLocationStatus = (id, status) => request.put(`/wms/location/${id}/status`, { status })
export const deleteLocation = (id) => request.delete(`/wms/location/${ids(id)}`)
/** 仓库下启用的库位：[{ id, code, name }] */
export const locationOptions = (warehouseId) => request.get('/wms/location/options', { params: { warehouseId } })
/** 上架推荐：data { warehouseId, lines: [{ materialId, batchNo }] } → 与各行对应的 [{ locationId, locationCode, reason }] */
export const suggestPutaway = (data) => request.post('/wms/location/putaway', data)
