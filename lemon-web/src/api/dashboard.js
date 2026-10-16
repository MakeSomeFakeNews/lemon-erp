import request from '@/utils/request'

export const getDashboard = () => request.get('/dashboard')
/** 我的待办：[{ module, title, count, link, level }] */
export const getTodos = () => request.get('/dashboard/todos')
/** 首页经营数据卡片（只返回有权限的项）：[{ key, label, kind: MONEY | COUNT, value, previous, link }] */
export const getBusinessCards = () => request.get('/dashboard/business')
