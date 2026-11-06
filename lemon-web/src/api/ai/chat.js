import request from '@/utils/request'
import { streamSse } from '@/utils/sse'

/** AI 问数：本人的会话 */
export const listConversations = () => request.get('/ai/chat/conversations')
export const createConversation = (title) => request.post('/ai/chat/conversations', { title })
export const renameConversation = (id, title) => request.put(`/ai/chat/conversations/${id}`, { title })
export const deleteConversation = (id) => request.delete(`/ai/chat/conversations/${id}`)
/** 消息历史：[{ id, role, content, toolName, toolLabel, hasToolCalls, tokens, createTime }]（工具结果不含在内） */
export const listMessages = (id) => request.get(`/ai/chat/conversations/${id}/messages`)
/** 工具消息的结果数据：{ messageId, tool, label, data, total, truncated, error } */
export const getToolData = (messageId) => request.get(`/ai/chat/messages/${messageId}/data`)

/**
 * 发送提问（SSE 流式回答）。事件：status { text, tool }、delta { text }、data { messageId, tool, label, data, total, truncated, error }、
 * done { messageId, usage, cancelled }、error { message }
 *
 * @param handlers { onEvent(name, data) }
 * @param signal   AbortSignal：停止生成
 */
export const sendMessage = (id, content, handlers, signal) =>
  streamSse(`/ai/chat/conversations/${id}/send`, { content }, handlers, signal)
