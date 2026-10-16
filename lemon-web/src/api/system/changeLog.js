import request from '@/utils/request'

/** 字段变更历史：{ bizType, bizId, current, size } */
export const pageChangeLog = (params) => request.get('/system/change-log', { params })
