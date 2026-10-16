import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

export const pageWarehouse = (params) => request.get('/wms/warehouse', { params })
export const getWarehouse = (id) => request.get(`/wms/warehouse/${id}`)
export const addWarehouse = (data) => request.post('/wms/warehouse', data)
export const updateWarehouse = (id, data) => request.put(`/wms/warehouse/${id}`, data)
export const changeWarehouseStatus = (id, status) => request.put(`/wms/warehouse/${id}/status`, { status })
export const deleteWarehouse = (id) => request.delete(`/wms/warehouse/${ids(id)}`)
/** 启用的仓库：[{ id, code, name }] */
export const warehouseOptions = () => request.get('/wms/warehouse/options')
