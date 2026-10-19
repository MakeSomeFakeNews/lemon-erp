import { del, get, post, put } from '@/utils/request'

/** 我参与的、未完成的项目 [{ id, code, name, status }] */
export const myProjects = () => get('/pm/project/options', { mine: true })
/** 项目任务树（需要项目、任务、工时查看等权限之一；没有权限时静默失败，改用我的任务） */
export const taskTree = (projectId) => get('/pm/task', { projectId }, { silent: true })
/** 分配给我的任务（登录即可） */
export const myTasks = () => get('/pm/my/tasks', null, { silent: true })

/** 本人工时：beginDate ~ endDate（最多 62 天） */
export const myTimesheet = (beginDate, endDate) => get('/pm/my/timesheet', { beginDate, endDate })
/** 填报 / 修改：{ projectId, taskId, workDate, hours, content }，返回 { id, warning } */
export const addTimesheet = (data) => post('/pm/my/timesheet', data)
export const updateTimesheet = (id, data) => put(`/pm/my/timesheet/${id}`, data)
export const deleteTimesheet = (id) => del(`/pm/my/timesheet/${id}`)

/** 工时审批列表：{ current, size, projectId, userId, status, beginDate, endDate } */
export const pageTimesheet = (params) => get('/pm/timesheet', params)
/** 审批：{ ids, approved, reason }，返回处理条数 */
export const reviewTimesheet = (data) => post('/pm/timesheet/review', data)
