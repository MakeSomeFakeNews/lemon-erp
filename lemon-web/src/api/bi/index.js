import request from '@/utils/request'

/** 多维分析 */
export const olapCubes = () => request.get('/bi/olap/cubes')
export const olapPivot = (data) => request.post('/bi/olap/pivot', data)

/** 自定义报表、仪表盘 */
export const listReports = (params) => request.get('/bi/report', { params })
export const getReport = (id) => request.get(`/bi/report/${id}`)
export const addReport = (data) => request.post('/bi/report', data)
export const updateReport = (id, data) => request.put(`/bi/report/${id}`, data)
export const deleteReport = (id) => request.delete(`/bi/report/${id}`)
export const listBoards = () => request.get('/bi/board')
export const getBoard = (id) => request.get(`/bi/board/${id}`)
export const addBoard = (data) => request.post('/bi/board', data)
export const updateBoard = (id, data) => request.put(`/bi/board/${id}`, data)
export const deleteBoard = (id) => request.delete(`/bi/board/${id}`)

/** 驾驶舱、决策支持、预警规则 */
export const cockpit = () => request.get('/bi/cockpit')
export const salesForecast = (params) => request.get('/bi/decision/forecast', { params })
export const abcAnalysis = () => request.get('/bi/decision/abc')
export const slowMoving = (days) => request.get('/bi/decision/slow-moving', { params: { days } })
export const alerts = () => request.get('/bi/decision/alerts')
export const listAlertRules = () => request.get('/bi/alert-rule')
export const alertMetrics = () => request.get('/bi/alert-rule/metrics')
export const addAlertRule = (data) => request.post('/bi/alert-rule', data)
export const updateAlertRule = (id, data) => request.put(`/bi/alert-rule/${id}`, data)
export const deleteAlertRule = (id) => request.delete(`/bi/alert-rule/${id}`)

/** 数据仓库 */
export const etlStatus = () => request.get('/bi/etl/status')
export const etlLogs = (params) => request.get('/bi/etl/logs', { params })
export const runEtl = () => request.post('/bi/etl/run')
