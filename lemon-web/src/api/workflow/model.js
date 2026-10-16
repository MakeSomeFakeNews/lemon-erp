import request from '@/utils/request'

export const pageModel = (params) => request.get('/workflow/model', { params })
export const getModel = (id) => request.get(`/workflow/model/${id}`)
export const addModel = (data) => request.post('/workflow/model', data)
export const updateModel = (id, data) => request.put(`/workflow/model/${id}`, data)
/** data：{ form: FormField[], flow: FlowNode } */
export const saveModelDesign = (id, data) => request.put(`/workflow/model/${id}/design`, data)
/** 发布已保存的设计，返回新的版本号 */
export const deployModel = (id) => request.post(`/workflow/model/${id}/deploy`)
export const changeModelStatus = (id, status) => request.put(`/workflow/model/${id}/status`, { status })
export const deleteModel = (ids) => request.delete(`/workflow/model/${[].concat(ids).join(',')}`)
