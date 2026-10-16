import request from '@/utils/request'

export const menuTree = () => request.get('/system/menu')
export const addMenu = (data) => request.post('/system/menu', data)
export const updateMenu = (id, data) => request.put(`/system/menu/${id}`, data)
export const deleteMenu = (id) => request.delete(`/system/menu/${id}`)
