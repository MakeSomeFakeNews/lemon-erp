import { getToken, removeToken } from './auth'
import { locale, t } from '@/locales'

/**
 * 后端接口：统一返回 { code, msg, data }，code 为 200 时返回 data；
 * 401 跳转登录，其他错误弹出提示并 reject（错误对象带 handled 标记，页面无需再提示）
 */
export const BASE_URL = import.meta.env.VITE_API_BASE || 'http://localhost:8080/api'
const LOGIN_PAGE = '/pages/login/index'

let redirecting = false

function toLogin() {
  removeToken()
  if (redirecting) return
  redirecting = true
  const pages = getCurrentPages()
  const current = pages[pages.length - 1]?.route
  if (current === LOGIN_PAGE.slice(1)) {
    redirecting = false
    return
  }
  uni.showToast({ title: t('common.relogin'), icon: 'none' })
  setTimeout(() => {
    uni.reLaunch({ url: LOGIN_PAGE, complete: () => (redirecting = false) })
  }, 800)
}

function fail(msg, silent) {
  if (!silent) uni.showToast({ title: msg, icon: 'none', duration: 2500 })
  const err = new Error(msg)
  err.handled = true
  return err
}

function headers() {
  const h = { 'Accept-Language': locale.value }
  const token = getToken()
  if (token) h.Authorization = `Bearer ${token}`
  return h
}

function handle(statusCode, res, silent) {
  if (statusCode === 401 || res?.code === 401) {
    toLogin()
    throw fail(res?.msg || t('common.relogin'), true)
  }
  if (statusCode === 403) throw fail(res?.msg || t('common.forbidden'), silent)
  if (res && res.code === 200) return res.data
  throw fail(res?.msg || t('common.serverError', { status: statusCode }), silent)
}

/**
 * @param {{ url: string, method?: string, data?: any, params?: object, silent?: boolean }} options
 *   params 拼接为查询参数（值为空的忽略）；silent 不弹出错误提示
 */
export function request({ url, method = 'GET', data, params, silent = false }) {
  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + url + query(params),
      method,
      data,
      header: headers(),
      timeout: 15000,
      success: ({ statusCode, data: res }) => {
        try {
          resolve(handle(statusCode, res, silent))
        } catch (e) {
          reject(e)
        }
      },
      fail: () => reject(fail(t('common.networkError'), silent))
    })
  })
}

/** 上传文件（表单字段 file），返回 data */
export function upload(url, filePath, formData = {}) {
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: BASE_URL + url,
      filePath,
      name: 'file',
      formData,
      header: headers(),
      success: ({ statusCode, data }) => {
        try {
          resolve(handle(statusCode, JSON.parse(data || '{}'), false))
        } catch (e) {
          reject(e)
        }
      },
      fail: () => reject(fail(t('common.networkError')))
    })
  })
}

/** 查询参数：忽略 null / undefined / 空字符串，数组按逗号连接 */
export function query(params) {
  if (!params) return ''
  const parts = Object.entries(params)
    .filter(([, v]) => v !== undefined && v !== null && v !== '')
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(Array.isArray(v) ? v.join(',') : v)}`)
  return parts.length ? `?${parts.join('&')}` : ''
}

/**
 * 后端文件地址（头像等，形如 /api/files/avatar/x.png）转为可访问的完整地址
 */
export function fileUrl(path) {
  if (!path || /^https?:\/\//.test(path)) return path || ''
  const origin = BASE_URL.replace(/\/api\/?$/, '')
  return path.startsWith('/api/') ? origin + path : BASE_URL + path
}

export const get = (url, params, opts) => request({ url, params, ...opts })
export const post = (url, data, opts) => request({ url, method: 'POST', data, ...opts })
export const put = (url, data, opts) => request({ url, method: 'PUT', data, ...opts })
export const del = (url, opts) => request({ url, method: 'DELETE', ...opts })
