import { get, put } from '@/utils/request'

/** 我的消息：{ current, size, unread } */
export const pageMessages = (params) => get('/system/message', params)
export const unreadCount = () => get('/system/message/unread-count', null, { silent: true })
/** 标为已读；不传 ids 为全部已读 */
export const readMessages = (ids) => put('/system/message/read', ids || [])
