import request from '@/utils/request'

export const getServerInfo = () => request.get('/monitor/server')
