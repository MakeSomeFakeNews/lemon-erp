import { BASE_URL } from '@/utils/request'
import { getToken } from '@/utils/auth'
import { batchFollowupPhotos } from '@/api/crm'

/**
 * 跟进照片：附件下载要带登录令牌，不能直接作为 <image> 的地址，先用 uni.downloadFile 下载为临时文件再显示。
 * 同一附件只下载一次（按附件 ID 缓存临时路径，小程序关闭后临时文件失效，缓存随之清空）
 */
const cache = new Map()

function download(id) {
  if (!cache.has(id)) {
    cache.set(
      id,
      new Promise((resolve) => {
        uni.downloadFile({
          url: `${BASE_URL}/system/attachment/${id}/download`,
          header: { Authorization: `Bearer ${getToken()}` },
          success: (r) => resolve(r.statusCode === 200 ? r.tempFilePath : null),
          fail: () => resolve(null)
        })
      }).then((path) => {
        if (!path) cache.delete(id)
        return path
      })
    )
  }
  return cache.get(id)
}

const isImage = (a) => (a.contentType || '').startsWith('image/') || /\.(jpe?g|png|gif|webp|heic)$/i.test(a.fileName || '')

/**
 * 列表中每条跟进各自请求照片时，同一时刻的请求合并为一次批量查询（避免一页几十个请求）
 */
let pending = new Map()
let timer = null

function attachmentsOf(followupId) {
  return new Promise((resolve) => {
    const key = String(followupId)
    if (!pending.has(key)) pending.set(key, [])
    pending.get(key).push(resolve)
    if (!timer) timer = setTimeout(flush, 0)
  })
}

async function flush() {
  const batch = pending
  pending = new Map()
  timer = null
  const byId = (await batchFollowupPhotos([...batch.keys()].map(Number)).catch(() => null)) || {}
  batch.forEach((resolvers, key) => resolvers.forEach((resolve) => resolve(byId[key] || [])))
}

/** 跟进记录的照片临时路径（按上传顺序），没有照片时为空数组 */
export async function followupPhotos(followupId) {
  const list = await attachmentsOf(followupId)
  const paths = await Promise.all(list.filter(isImage).map((a) => download(a.id)))
  return paths.filter(Boolean)
}

export const MAX_PHOTOS = 9

/** 拍照或从相册选择（压缩），返回临时路径 */
export function choosePhotos(count) {
  return new Promise((resolve) => {
    uni.chooseImage({
      count,
      sizeType: ['compressed'],
      sourceType: ['camera', 'album'],
      success: (r) => resolve(r.tempFilePaths || []),
      fail: () => resolve([])
    })
  })
}
