import request from '@/utils/request'

export const listAttachment = (bizType, bizId) => request.get('/system/attachment', { params: { bizType, bizId } })
export const uploadAttachment = (bizType, bizId, file) => {
  const data = new FormData()
  data.append('bizType', bizType)
  data.append('bizId', bizId)
  data.append('file', file)
  return request.post('/system/attachment', data, { timeout: 120000 })
}
/** 下载为 Blob（需要登录态，不能直接用链接） */
export const downloadAttachment = (id) =>
  request.get(`/system/attachment/${id}/download`, { responseType: 'blob', timeout: 120000 })
export const deleteAttachment = (id) => request.delete(`/system/attachment/${id}`)
