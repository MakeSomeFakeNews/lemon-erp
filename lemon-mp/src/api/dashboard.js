import { get } from '@/utils/request'

/** 首页待办 [{ module, title, count, link, level }]（按当前用户的权限） */
export const getTodos = () => get('/dashboard/todos', null, { silent: true })
/** 经营数据卡片 [{ key, label, kind: MONEY | COUNT, value, previous, link }] */
export const getBusiness = () => get('/dashboard/business', null, { silent: true })
