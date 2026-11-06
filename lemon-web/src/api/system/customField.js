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

/** 业务参数（按租户）：[{ key, module, label, type, value, defaultValue, modified, options, min, max, remark }] */
export const listBizRules = () => request.get('/system/biz-rule')
/** 保存业务参数：{ 参数标识: 值 }，只包含要修改的 */
export const saveBizRules = (data) => request.put('/system/biz-rule', data)
/** 登录用户可读的业务参数（各模块声明为前端可读的）：{ 参数标识: 值 }，前端用 useBizRules 读取 */
export const bizRuleValues = (keys) => request.get('/system/biz-rule/values', { params: { keys: keys.join(',') } })
