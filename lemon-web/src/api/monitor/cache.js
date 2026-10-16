import request from '@/utils/request'

export const getCacheInfo = () => request.get('/monitor/cache')
export const listCacheNames = () => request.get('/monitor/cache/names')
export const listCacheKeys = (prefix) => request.get('/monitor/cache/keys', { params: { prefix } })
export const getCacheValue = (key) => request.get('/monitor/cache/value', { params: { key } })
export const clearCache = (prefix) => request.delete('/monitor/cache/names', { params: { prefix } })
export const deleteCacheKey = (key) => request.delete('/monitor/cache/keys', { params: { key } })
