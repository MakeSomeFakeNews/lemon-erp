import request from '@/utils/request'

/** 通知公告 - 管理端（sys:notice:*） */
export const pageNotice = (params) => request.get('/system/notice', { params })
export const getNotice = (id) => request.get(`/system/notice/${id}`)
export const noticeOptions = () => request.get('/system/notice/options')
export const addNotice = (data) => request.post('/system/notice', data)
export const updateNotice = (id, data) => request.put(`/system/notice/${id}`, data)
export const deleteNotice = (ids) => request.delete(`/system/notice/${[].concat(ids).join(',')}`)
/** 发布，返回接收人数 */
export const publishNotice = (id) => request.put(`/system/notice/${id}/publish`)
export const withdrawNotice = (id) => request.put(`/system/notice/${id}/withdraw`)
/** 接收人名单：{ read: true 已读 / false 未读 / 不传全部, keyword, current, size } */
export const noticeReaders = (id, params) => request.get(`/system/notice/${id}/readers`, { params })

/** 通知公告 - 员工端（登录即可，只有发给自己的有效公告） */
export const pageMyNotice = (params) => request.get('/system/notice/my', { params })
export const myNoticeUnreadCount = () => request.get('/system/notice/my/unread-count')
export const getMyNotice = (id) => request.get(`/system/notice/my/${id}`)
export const readMyNotice = (id) => request.put(`/system/notice/my/${id}/read`)
