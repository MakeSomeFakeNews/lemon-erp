import request from '@/utils/request'

export const pageOperLog = (params) => request.get('/monitor/operlog', { params })
export const getOperLog = (id) => request.get(`/monitor/operlog/${id}`)
export const deleteOperLog = (ids) => request.delete(`/monitor/operlog/${[].concat(ids).join(',')}`)
export const cleanOperLog = () => request.delete('/monitor/operlog/clean')
