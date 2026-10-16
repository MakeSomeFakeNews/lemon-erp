import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

export const pageMaterial = (params) => request.get('/wms/material', { params })
export const getMaterial = (id) => request.get(`/wms/material/${id}`)
export const addMaterial = (data) => request.post('/wms/material', data)
export const updateMaterial = (id, data) => request.put(`/wms/material/${id}`, data)
export const changeMaterialStatus = (id, status) => request.put(`/wms/material/${id}/status`, { status })
export const deleteMaterial = (id) => request.delete(`/wms/material/${ids(id)}`)
/** 按编码或名称搜索启用的物料，最多 50 条 */
export const materialOptions = (keyword) => request.get('/wms/material/options', { params: { keyword } })
/** Excel 导入：{ rows: [{ rowNum, data }] } */
export const importMaterial = (data) => request.post('/wms/material/import', data)
/** 辅助单位：[{ id, materialId, unit, ratio, barcode }]，1 辅助单位 = ratio 基本单位 */
export const materialUnits = (id) => request.get(`/wms/material/${id}/units`)
/** 整体保存辅助单位：[{ unit, ratio, barcode }] */
export const saveMaterialUnits = (id, data) => request.put(`/wms/material/${id}/units`, data)
/** 扫码：按物料或辅助单位条码查找，{ material, unit, ratio }，找不到返回 null */
export const materialByBarcode = (code) => request.get('/wms/material/barcode', { params: { code } })
