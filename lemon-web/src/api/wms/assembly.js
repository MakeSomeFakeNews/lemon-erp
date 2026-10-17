import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

/** 组装拆卸单：params { docNo, bizType（种类）, warehouseId, status, beginDate, endDate } */
export const pageAssembly = (params) => request.get('/wms/assembly', { params })
export const getAssembly = (id) => request.get(`/wms/assembly/${id}`)
export const addAssembly = (data) => request.post('/wms/assembly', data)
export const updateAssembly = (id, data) => request.put(`/wms/assembly/${id}`, data)
export const confirmAssembly = (id) => request.post(`/wms/assembly/${id}/confirm`)
export const deleteAssembly = (id) => request.delete(`/wms/assembly/${ids(id)}`)
