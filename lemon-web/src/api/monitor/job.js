import request from '@/utils/request'

export const listJob = () => request.get('/monitor/job')
export const updateJob = (id, data) => request.put(`/monitor/job/${id}`, data)
export const runJob = (id) => request.post(`/monitor/job/${id}/run`)
export const pageJobLog = (params) => request.get('/monitor/job/logs', { params })
