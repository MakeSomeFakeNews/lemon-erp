import axios from 'axios'
import { message } from 'ant-design-vue'
import { getToken, removeToken } from './auth'
import { currentLocale, t } from '@/locales'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  // 后端按标准请求头 Accept-Language 返回对应语言的提示（浏览器默认的值是系统语言，这里改为界面语言）
  config.headers['Accept-Language'] = currentLocale()
  return config
})

/**
 * 拦截器已经给出提示的错误打上标记，页面里 try/finally 不必再 catch，
 * 由 main.js 的全局错误处理统一忽略
 */
export function isHandledError(err) {
  return !!err?.handled
}

function reject(err) {
  err.handled = true
  return Promise.reject(err)
}

/** 同一条错误提示用 content 作 key，并发请求同时失败时只显示一条 */
function showError(content) {
  message.error({ content, key: content })
}

// 多个并发请求同时 401 时只处理一次
let redirecting = false

function toLogin() {
  if (redirecting) return
  redirecting = true
  removeToken()
  if (location.pathname.startsWith('/login')) {
    redirecting = false
    return
  }
  message.warning({ content: t('layout.relogin'), key: 'relogin', duration: 1.5 })
  setTimeout(() => {
    location.href = `/login?redirect=${encodeURIComponent(location.pathname + location.search)}`
  }, 800)
}

request.interceptors.response.use(
  async (response) => {
    // 文件下载：成功时返回 Blob；失败时后端返回 JSON 错误
    if (response.config.responseType === 'blob') {
      if (!response.data.type?.includes('application/json')) return response.data
      response = { ...response, data: JSON.parse(await response.data.text()) }
    }
    const res = response.data
    if (res.code === 200) {
      return res.data
    }
    if (res.code === 401) {
      toLogin()
    } else {
      showError(res.msg || t('layout.requestFailed'))
    }
    return reject(new Error(res.msg))
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      toLogin()
    } else if (status === 403) {
      showError(t('layout.forbidden'))
    } else if (error.code === 'ECONNABORTED') {
      showError(t('layout.timeout'))
    } else {
      showError(error.response?.data?.msg || (status ? t('layout.serverError', { status }) : t('layout.networkError')))
    }
    return reject(error)
  }
)

export default request
