import request from '@/utils/request'

/** 生产看板：{ workCenterId }，为空看全部产线 */
export const getBoard = (params) => request.get('/mes/board', { params })
