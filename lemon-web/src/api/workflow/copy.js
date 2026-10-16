import request from '@/utils/request'

export const pageCopy = (params) => request.get('/workflow/copy', { params })
