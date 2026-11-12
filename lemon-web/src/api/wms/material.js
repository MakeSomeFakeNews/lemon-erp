import request from '@/utils/request'
import { batchByIds } from '@/utils/batchByIds'

const ids = (v) => [].concat(v).join(',')

export const pageMaterial = (params) => request.get('/wms/material', { params })
export const getMaterial = (id) => request.get(`/wms/material/${id}`)
/** 新增；copyFrom 为复制的原物料 ID（同时复制其辅助单位（不含包装条码）和图片） */
export const addMaterial = (data, copyFrom) => request.post('/wms/material', data, { params: copyFrom ? { copyFrom } : undefined })
export const updateMaterial = (id, data) => request.put(`/wms/material/${id}`, data)
export const changeMaterialStatus = (id, status) => request.put(`/wms/material/${id}/status`, { status })
export const deleteMaterial = (id) => request.delete(`/wms/material/${ids(id)}`)
/**
 * 物料下拉：启用的物料按关键字（编码、名称、规格、条码、拼音首字母、去掉分隔符的缩写）分页搜索；
 * page：{ current, size }（每页最多 50，默认 20）
 */
export const materialOptions = (keyword, page) => request.get('/wms/material/options', { params: { keyword, ...page } })
/** 按 ID 取物料（含停用的），下拉回显已选的值 */
export const materialsByIds = batchByIds((idList) => request.get('/wms/material/options', { params: { ids: ids(idList) } }))
/** Excel 导入：{ rows: [{ rowNum, data }] } */
export const importMaterial = (data) => request.post('/wms/material/import', data)
/** 辅助单位：[{ id, materialId, unit, ratio, barcode }]，1 辅助单位 = ratio 基本单位 */
export const materialUnits = (id) => request.get(`/wms/material/${id}/units`)
/** 整体保存辅助单位：[{ unit, ratio, barcode }] */
export const saveMaterialUnits = (id, data) => request.put(`/wms/material/${id}/units`, data)
/** 扫码：按物料或辅助单位条码查找，{ material, unit, ratio }，找不到返回 null */
export const materialByBarcode = (code) => request.get('/wms/material/barcode', { params: { code } })
/**
 * 批量设置库存预警（issue #111）：{ materialIds, alertMode（TOTAL 按总库存 / WAREHOUSE 分仓库）, warehouseId（分仓库时）, minQty, maxQty } → 设置的物料数。
 * 按总库存清除分仓库的设置；分仓库只改所选仓库（上下限都为空时删除该仓库的设置），物料改为分仓库预警
 */
export const batchSetStockLimits = (data) => request.put('/wms/material/stock-limits', data)

/** 规格属性：[{ id, name, sort, status, values: [{ id, value, code }] }] */
export const attributes = () => request.get('/wms/attribute')
export const addAttribute = (data) => request.post('/wms/attribute', data)
export const updateAttribute = (id, data) => request.put(`/wms/attribute/${id}`, data)
export const deleteAttribute = (id) => request.delete(`/wms/attribute/${id}`)
/** 款式（多规格物料）搜索 */
export const templateOptions = (keyword) => request.get('/wms/material/templates', { params: { keyword } })
/** 款式的规格矩阵：{ template, attributes: [{ id, name, values }], variants: [{ id, code, spec, variantText, values: { 属性ID: 值ID } }] } */
export const materialVariants = (id) => request.get(`/wms/material/${id}/variants`)
/** 款式各 SKU 的库存：{ SKU ID: 数量 } */
export const variantStock = (id) => request.get(`/wms/material/${id}/variant-stock`)
/** 生成 SKU：valueIds { 属性ID: [值ID] } → 新生成的数量 */
export const generateVariants = (id, valueIds) => request.post(`/wms/material/${id}/variants`, { valueIds })

/** 批次 / 序列号管理切换前检查：{ batchEnabled, serialEnabled, hasTransactions, stockQty, noBatchQty, batchCount, openDocs } */
export const materialTracking = (id) => request.get(`/wms/material/${id}/tracking`)
/** 切换批次 / 序列号管理：{ batchEnabled, serialEnabled, batchNo, productionDate, expiryDate } */
export const changeMaterialTracking = (id, data) => request.put(`/wms/material/${id}/tracking`, data)

/** 商品图片 [{ id, url, fileName, fileSize, sort }]，第一张为主图 */
export const materialImages = (id) => request.get(`/wms/material/${id}/images`)
export const uploadMaterialImage = (id, file) => {
  const data = new FormData()
  data.append('file', file)
  return request.post(`/wms/material/${id}/images`, data, { timeout: 120000 })
}
/** 调整顺序：全部图片 ID，第一张为主图 */
export const reorderMaterialImages = (id, imageIds) => request.put(`/wms/material/${id}/images/order`, imageIds)
export const setMainMaterialImage = (id, imageId) => request.put(`/wms/material/${id}/images/${imageId}/main`)
export const deleteMaterialImage = (id, imageId) => request.delete(`/wms/material/${id}/images/${imageId}`)
