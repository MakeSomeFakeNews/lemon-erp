import request from '@/utils/request'

export const deptTree = () => request.get('/system/dept')
export const addDept = (data) => request.post('/system/dept', data)
export const updateDept = (id, data) => request.put(`/system/dept/${id}`, data)
export const deleteDept = (id) => request.delete(`/system/dept/${id}`)
