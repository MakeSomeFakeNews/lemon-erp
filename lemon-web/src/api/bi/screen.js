import axios from 'axios'
import request from '@/utils/request'
import { getToken } from '@/utils/auth'
import { currentLocale, t } from '@/locales'

/**
 * 数据大屏的数据请求：大屏无人值守、定时刷新，失败时不弹提示（由页面显示离线状态并保留上次的数据），
 * 投屏链接不带登录令牌（免登录访问，避免过期的令牌影响）
 */
const screenHttp = axios.create({ baseURL: '/api', timeout: 20000 })

async function get(url, { params, auth } = {}) {
  const token = auth ? getToken() : null
  try {
    const { data } = await screenHttp.get(url, { params, headers: { 'X-Lang': currentLocale(), ...(token ? { Authorization: `Bearer ${token}` } : {}) } })
    if (data.code === 200) return data.data
    throw Object.assign(new Error(data.msg || t('layout.requestFailed')), { code: data.code })
  } catch (e) {
    if (e.code !== undefined && typeof e.code === 'number') throw e
    const status = e.response?.status
    throw Object.assign(new Error(status === 401 ? t('layout.relogin') : status === 403 ? t('layout.forbidden') : t('layout.networkError')), { code: status })
  }
}

/** 登录后查看：key 为 factory / warehouse / sales / quality */
export const screenData = (key, params) => get(`/bi/screen/${key.toLowerCase()}`, { params, auth: true })
/** 投屏链接（免登录） */
export const shareInfo = (token) => get(`/screen-share/${token}`)
export const shareData = (token, key) => get(`/screen-share/${token}/${key.toLowerCase()}`)

/** 投屏链接管理 */
export const listShares = () => request.get('/bi/screen/share')
export const createShare = (data) => request.post('/bi/screen/share', data)
export const updateShare = (id, data) => request.put(`/bi/screen/share/${id}`, data)
export const resetShare = (id) => request.post(`/bi/screen/share/${id}/reset`)
export const deleteShare = (id) => request.delete(`/bi/screen/share/${id}`)
