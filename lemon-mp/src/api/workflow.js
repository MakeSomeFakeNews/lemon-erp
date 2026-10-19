import { get, post } from '@/utils/request'

/** 我的待办：{ current, size, name } → { records: TaskItem[], total } */
export const pageTodo = (params) => get('/workflow/task/todo', params)
/** 我的已办 */
export const pageDone = (params) => get('/workflow/task/done', params)
/** 我的申请：{ current, size, name, status } → { records: InstanceItem[], total } */
export const pageMine = (params) => get('/workflow/instance/mine', params)
/** 流程实例详情（表单、业务信息、审批记录、todoTaskId、cancelable） */
export const getInstance = (id) => get(`/workflow/instance/${id}`)

export const approveTask = (taskId, comment) => post(`/workflow/task/${taskId}/approve`, { comment })
export const rejectTask = (taskId, comment) => post(`/workflow/task/${taskId}/reject`, { comment })
export const transferTask = (taskId, userId, comment) => post(`/workflow/task/${taskId}/transfer`, { userId, comment })
export const cancelInstance = (id, reason) => post(`/workflow/instance/${id}/cancel`, { reason })

/** 转交时选择用户（登录即可访问，只返回启用的用户）：{ current, size, username } */
export const pageIdentityUsers = (params) => get('/workflow/identity/users', params)

/** 抄送我的：{ current, size, name } → { records: WfCopy[], total } */
export const pageCopy = (params) => get('/workflow/copy', params)

/** 可发起的流程（不含业务单据接入的流程） */
export const listStartModels = () => get('/workflow/start/models')
/** 发起页面：{ processDefinitionId, modelKey, name, form: FormField[], flow } */
export const getStartModel = (modelKey) => get(`/workflow/start/models/${encodeURIComponent(modelKey)}`)
/** 发起流程：{ modelKey, formData }，返回流程实例 ID */
export const startInstance = (data) => post('/workflow/instance', data)
