import { get, put } from '@/utils/request'

/** 通知公告（员工端，与 Web 端共用）：只有发给自己、已发布且未过期的公告 */
export const pageNotices = (params) => get('/system/notice/my', params)
export const noticeUnreadCount = () => get('/system/notice/my/unread-count', null, { silent: true })
export const getNotice = (id) => get(`/system/notice/my/${id}`)
/** 标为已读，对应的站内消息一并已读 */
export const readNotice = (id) => put(`/system/notice/my/${id}/read`)
export const noticeAttachments = (id) => get('/system/attachment', { bizType: 'sys_notice', bizId: id })
