import request from '@/utils/request'

/** AI 设置：{ provider, baseUrl, chatModel, visionModel, enabled, monthlyTokenLimit, chatEnabled, ..., apiKeySet, apiKeyMask }，API Key 不返回 */
export const getAiConfig = () => request.get('/ai/config')
/** 服务商及默认接口地址：[{ value, defaultBaseUrl }] */
export const listAiProviders = () => request.get('/ai/config/providers')
/** 保存：apiKey 为空表示不修改 */
export const saveAiConfig = (data) => request.put('/ai/config', data)
/** 测试连接（用表单中的配置，apiKey 为空时用已保存的）：{ reply, model, latencyMs, promptTokens, completionTokens } */
export const testAiConfig = (data) => request.post('/ai/config/test', data, { timeout: 150000 })
/** 本月用量：{ month, totalTokens, limit, byFeature: [{ key, calls, promptTokens, completionTokens }], byUser: [{ key, name, ... }] } */
export const getAiUsage = () => request.get('/ai/config/usage')
/** 当前租户各 AI 功能是否可用：{ configured, enabled, vision, features: { CHAT: true, ... } } */
export const getAiStatus = () => request.get('/ai/config/status')
