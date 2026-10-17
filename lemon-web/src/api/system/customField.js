import request from '@/utils/request'

/** 可扩展字段的业务类型：[{ code, name }] */
export const customFieldTypes = () => request.get('/system/custom-field/types')
/** 全部定义（管理页面） */
export const allCustomFields = (bizType) => request.get('/system/custom-field/all', { params: { bizType } })
/** 业务类型启用的字段 */
export const customFields = (bizType) => request.get('/system/custom-field', { params: { bizType } })
export const addCustomField = (data) => request.post('/system/custom-field', data)
export const updateCustomField = (id, data) => request.put(`/system/custom-field/${id}`, data)
export const deleteCustomField = (id) => request.delete(`/system/custom-field/${id}`)
/** 一条记录的值：{ 字段标识: 值 } */
export const customValues = (bizType, bizId) => request.get('/system/custom-field/values', { params: { bizType, bizId } })
/** 多条记录的值：{ 记录ID: { 字段标识: 值 } } */
export const batchCustomValues = (bizType, bizIds) => request.post('/system/custom-field/values/batch', bizIds, { params: { bizType } })
export const saveCustomValues = (bizType, bizId, values) => request.put('/system/custom-field/values', values, { params: { bizType, bizId } })
