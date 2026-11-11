import request from '@/utils/request'

/** 全部物料分类（含停用的）：[{ id, parentId, code, name, sort, status, builtin, remark }]，按树的先序 */
export const listMaterialCategories = () => request.get('/wms/material-category')
/** 各分类直接包含的物料数：{ 编码: 数量 } */
export const materialCategoryCounts = () => request.get('/wms/material-category/counts')
/** 新增分类：{ parentId, code（为空自动生成）, name, sort, status, remark }，返回 ID */
export const addMaterialCategory = (data) => request.post('/wms/material-category', data)
/** 修改分类（编码不能修改） */
export const updateMaterialCategory = (id, data) => request.put(`/wms/material-category/${id}`, data)
/** 拖动排序：{ parentId, ids }，ids 按新顺序排在 parentId 下 */
export const sortMaterialCategories = (data) => request.put('/wms/material-category/sort', data)
export const deleteMaterialCategory = (id) => request.delete(`/wms/material-category/${id}`)
