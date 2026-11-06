import request from '@/utils/request'

/** 智能报告：当前用户能看到的报告（不含正文）{ records, total }，periodType 为 WEEK / MONTH 或空 */
export const pageReports = (params) => request.get('/ai/report', { params })
/** 生成报告时会包含的数据来源：[{ key, label, allData }] */
export const listReportSources = () => request.get('/ai/report/sources')
/** 报告详情（含正文 content，Markdown） */
export const getReport = (id) => request.get(`/ai/report/${id}`)
/** 报告依据的数据快照（JSON） */
export const getReportSnapshot = (id) => request.get(`/ai/report/${id}/snapshot`)
/** 生成报告 { periodType, date }，返回报告 ID（后台撰写，状态为 GENERATING） */
export const generateReport = (data) => request.post('/ai/report', data, { timeout: 60000 })
export const deleteReport = (id) => request.delete(`/ai/report/${id}`)
/** 经营预警的 AI 分析：{ ruleId, content, sourceLabels, tokens } */
export const analyzeAlert = (ruleId) => request.post(`/ai/report/alerts/${ruleId}/analysis`, null, { timeout: 150000 })
