import request from '@/utils/request'

export const pageTodoTask = (params) => request.get('/workflow/task/todo', { params })
export const pageDoneTask = (params) => request.get('/workflow/task/done', { params })
export const approveTask = (taskId, comment) => request.post(`/workflow/task/${taskId}/approve`, { comment })
export const rejectTask = (taskId, comment) => request.post(`/workflow/task/${taskId}/reject`, { comment })
export const transferTask = (taskId, userId, comment) =>
  request.post(`/workflow/task/${taskId}/transfer`, { userId, comment })
