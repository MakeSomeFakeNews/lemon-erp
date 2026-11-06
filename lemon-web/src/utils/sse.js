import { message } from 'ant-design-vue'
import { getToken, removeToken } from './auth'
import { currentLocale, t } from '@/locales'

/**
 * 用 fetch 发送 POST 并读取 SSE（text/event-stream）：axios 不支持流式读取，EventSource 不能 POST、不能带请求头。
 * 后端在开始流式输出前校验失败时返回普通 JSON（R），这里按 code 提示，与 utils/request.js 的处理一致。
 *
 * @param url      相对 /api 的地址
 * @param body     请求体（JSON）
 * @param handlers { onEvent(name, data) }：data 为解析后的 JSON
 * @param signal   AbortSignal，中止时不提示错误
 * @returns Promise：流结束时 resolve；请求失败时 reject（已提示，带 handled 标记）
 */
export async function streamSse(url, body, { onEvent }, signal) {
  const headers = {
    'Content-Type': 'application/json',
    Accept: 'text/event-stream, application/json',
    'Accept-Language': currentLocale()
  }
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`
  let res
  try {
    res = await fetch(`/api${url}`, { method: 'POST', headers, body: JSON.stringify(body), signal })
  } catch (e) {
    if (e.name === 'AbortError') return
    return fail(t('layout.networkError'))
  }
  const type = res.headers.get('content-type') || ''
  if (!type.includes('text/event-stream')) {
    let data = {}
    try {
      data = await res.json()
    } catch {
      // 非 JSON 响应
    }
    if (res.status === 401 || data.code === 401) {
      removeToken()
      location.href = `/login?redirect=${encodeURIComponent(location.pathname + location.search)}`
      return
    }
    if (res.status === 403 || data.code === 403) return fail(t('layout.forbidden'))
    return fail(data.msg || t('layout.serverError', { status: res.status }))
  }
  const reader = res.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  try {
    for (;;) {
      const { value, done } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n')
      let index
      while ((index = buffer.indexOf('\n\n')) >= 0) {
        dispatch(buffer.slice(0, index), onEvent)
        buffer = buffer.slice(index + 2)
      }
    }
    if (buffer.trim()) dispatch(buffer, onEvent)
  } catch (e) {
    if (e.name !== 'AbortError') throw e
  }
}

function dispatch(block, onEvent) {
  let name = 'message'
  const data = []
  for (const line of block.split('\n')) {
    if (line.startsWith('event:')) name = line.slice(6).trim()
    else if (line.startsWith('data:')) data.push(line.slice(5).replace(/^ /, ''))
  }
  if (!data.length) return
  let parsed
  try {
    parsed = JSON.parse(data.join('\n'))
  } catch {
    parsed = data.join('\n')
  }
  onEvent(name, parsed)
}

function fail(msg) {
  message.error({ content: msg, key: msg })
  const err = new Error(msg)
  err.handled = true
  return Promise.reject(err)
}
