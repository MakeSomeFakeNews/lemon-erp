import request from '@/utils/request'

export const listOnline = (params) => request.get('/monitor/online', { params })
export const kickOnline = (tokenId) => request.delete(`/monitor/online/${tokenId}`)
