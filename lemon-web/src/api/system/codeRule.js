import request from '@/utils/request'

export const listCodeRules = () => request.get('/system/code-rule')
export const updateCodeRule = (key, data) => request.put(`/system/code-rule/${key}`, data)
export const resetCodeRule = (key) => request.delete(`/system/code-rule/${key}`)
