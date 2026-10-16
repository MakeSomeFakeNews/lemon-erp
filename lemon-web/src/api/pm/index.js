import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

/** 项目 */
export const pageProject = (params) => request.get('/pm/project', { params })
export const projectOptions = (mine) => request.get('/pm/project/options', { params: { mine } })
export const getProject = (id) => request.get(`/pm/project/${id}`)
export const addProject = (data) => request.post('/pm/project', data)
export const updateProject = (id, data) => request.put(`/pm/project/${id}`, data)
export const changeProjectStatus = (id, status) => request.put(`/pm/project/${id}/status`, { status })
export const deleteProject = (id) => request.delete(`/pm/project/${ids(id)}`)

/** 成员与资源 */
export const listMembers = (projectId) => request.get(`/pm/project/${projectId}/member`)
export const addMember = (projectId, data) => request.post(`/pm/project/${projectId}/member`, data)
export const updateMember = (projectId, id, data) => request.put(`/pm/project/${projectId}/member/${id}`, data)
export const deleteMember = (projectId, id) => request.delete(`/pm/project/${projectId}/member/${id}`)
export const listResources = (deptId) => request.get('/pm/resource', { params: { deptId } })

/** 任务 */
export const taskTree = (projectId) => request.get('/pm/task', { params: { projectId } })
export const addTask = (data) => request.post('/pm/task', data)
export const updateTask = (id, data) => request.put(`/pm/task/${id}`, data)
export const deleteTask = (id) => request.delete(`/pm/task/${id}`)
export const updateTaskProgress = (id, progress) => request.put(`/pm/task/${id}/progress`, { progress })
export const myTasks = () => request.get('/pm/my/tasks')

/** 工时 */
export const pageTimesheet = (params) => request.get('/pm/timesheet', { params })
export const reviewTimesheet = (data) => request.post('/pm/timesheet/review', data)
export const myTimesheet = (beginDate, endDate) => request.get('/pm/my/timesheet', { params: { beginDate, endDate } })
export const addTimesheet = (data) => request.post('/pm/my/timesheet', data)
export const updateTimesheet = (id, data) => request.put(`/pm/my/timesheet/${id}`, data)
export const deleteTimesheet = (id) => request.delete(`/pm/my/timesheet/${id}`)

/** 成本控制 */
export const pageCost = (params) => request.get('/pm/cost', { params })
export const addCost = (data) => request.post('/pm/cost', data)
export const deleteCost = (id) => request.delete(`/pm/cost/${ids(id)}`)
export const listBudgets = (projectId) => request.get(`/pm/project/${projectId}/budget`)
export const saveBudgets = (projectId, items) => request.put(`/pm/project/${projectId}/budget`, { items })
export const issueMaterial = (projectId, data) => request.post(`/pm/project/${projectId}/issue`, data)

/** 项目核算与收款计划 */
export const pageAccounting = (params) => request.get('/pm/accounting', { params })
export const getAccounting = (projectId) => request.get(`/pm/accounting/${projectId}`)
export const accountingMonthly = (projectId) => request.get(`/pm/accounting/${projectId}/monthly`)
/** 项目各期月结（收入确认、成本结转、人工费用入账） */
export const accountingRevenue = (projectId) => request.get(`/pm/accounting/${projectId}/revenue`)
/** 项目月结：返回生成的凭证数 */
export const closeAccounting = (period) => request.post('/pm/accounting/close', { period })
export const listBillings = (projectId) => request.get('/pm/billing', { params: { projectId } })
export const addBilling = (data) => request.post('/pm/billing', data)
export const updateBilling = (id, data) => request.put(`/pm/billing/${id}`, data)
export const deleteBilling = (id) => request.delete(`/pm/billing/${id}`)
export const billBilling = (id, billDate) => request.post(`/pm/billing/${id}/bill`, { billDate })
export const unbillBilling = (id) => request.post(`/pm/billing/${id}/unbill`)
