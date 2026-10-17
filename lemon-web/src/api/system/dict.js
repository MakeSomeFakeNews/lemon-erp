import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

// 字典类型
export const pageDictType = (params) => request.get('/system/dict/type', { params })
export const getDictType = (id) => request.get(`/system/dict/type/${id}`)
export const addDictType = (data) => request.post('/system/dict/type', data)
export const updateDictType = (id, data) => request.put(`/system/dict/type/${id}`, data)
export const deleteDictType = (id) => request.delete(`/system/dict/type/${ids(id)}`)

// 字典数据
export const pageDictData = (params) => request.get('/system/dict/data', { params })
export const addDictData = (data) => request.post('/system/dict/data', data)
export const updateDictData = (id, data) => request.put(`/system/dict/data/${id}`, data)
export const deleteDictData = (id) => request.delete(`/system/dict/data/${ids(id)}`)

// 业务字典（当前租户的取值）
export const listBizDictTypes = () => request.get('/system/biz-dict/types')
export const pageBizDictData = (params) => request.get('/system/biz-dict/data', { params })
export const addBizDictData = (data) => request.post('/system/biz-dict/data', data)
export const updateBizDictData = (id, data) => request.put(`/system/biz-dict/data/${id}`, data)
export const deleteBizDictData = (id) => request.delete(`/system/biz-dict/data/${ids(id)}`)

/** 某类型下启用的字典数据，前端字典缓存使用 */
export const getDictDataByType = (dictType) => request.get(`/system/dict/data/type/${dictType}`)
