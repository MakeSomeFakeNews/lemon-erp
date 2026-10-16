import request from '@/utils/request'

export const pageCheck = (params) => request.get('/wms/check', { params })
export const getCheck = (id) => request.get(`/wms/check/${id}`)
/** 按仓库当前库存生成盘点明细（不保存）：{ warehouseId, locationId?, keyword?, category? } */
export const checkSnapshot = (params) => request.get('/wms/check/snapshot', { params })
export const addCheck = (data) => request.post('/wms/check', data)
export const updateCheck = (id, data) => request.put(`/wms/check/${id}`, data)
export const confirmCheck = (id) => request.post(`/wms/check/${id}/confirm`)
export const deleteCheck = (ids) => request.delete(`/wms/check/${[].concat(ids).join(',')}`)
