import request from '@/utils/request'

export const pageLoginLog = (params) => request.get('/monitor/loginlog', { params })
export const deleteLoginLog = (ids) => request.delete(`/monitor/loginlog/${[].concat(ids).join(',')}`)
export const cleanLoginLog = () => request.delete('/monitor/loginlog/clean')
