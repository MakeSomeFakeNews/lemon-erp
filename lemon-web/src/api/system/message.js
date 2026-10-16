import request from '@/utils/request'

/** 我的消息 */
export const pageMessage = (params) => request.get('/system/message', { params })
export const unreadCount = () => request.get('/system/message/unread-count')
/** ids 为空时全部已读 */
export const readMessage = (ids = []) => request.put('/system/message/read', [].concat(ids))
export const deleteMessage = (ids) => request.delete(`/system/message/${[].concat(ids).join(',')}`)
