import request from '@/utils/request'

export const pageCheck = (params) => request.get('/wms/check', { params })
export const getCheck = (id) => request.get(`/wms/check/${id}`)
/**
 * 选择盘点的物料（物料资料）：{ warehouseId, warehouseGoods, keyword, category, brand, locationId, changed, changeBegin, changeEnd,
 * purchaseBegin, purchaseEnd, salesBegin, salesEnd, status, stock（StockSign） }
 */
export const pageCheckMaterials = (params) => request.get('/wms/check/materials', { params })
/** 把选择的物料转为盘点明细（不保存），带出账面数量：{ warehouseId, materialIds }，每次最多 500 个（手机端扫码 / 搜索添加物料时也用） */
export const checkMaterialItems = (data) => request.post('/wms/check/materials/items', data)
export const addCheck = (data) => request.post('/wms/check', data)
export const updateCheck = (id, data) => request.put(`/wms/check/${id}`, data)
export const confirmCheck = (id) => request.post(`/wms/check/${id}/confirm`)
export const deleteCheck = (ids) => request.delete(`/wms/check/${[].concat(ids).join(',')}`)
