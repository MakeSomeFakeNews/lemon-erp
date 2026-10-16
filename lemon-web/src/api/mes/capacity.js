import request from '@/utils/request'

/** 工作中心负荷：{ from, to, workCenterId }，区间不超过 92 天 */
export const capacityLoad = (params) => request.get('/mes/capacity/load', { params })
/** 单个工单排程，返回工单 */
export const scheduleWo = (woId) => request.post(`/mes/capacity/schedule/${woId}`)
/** 全部未结束工单重新排程，返回工单数 */
export const scheduleAll = () => request.post('/mes/capacity/schedule-all')

/** 工厂日历：{ from, to, workCenterId } */
export const listCalendar = (params) => request.get('/mes/capacity/calendar', { params })
/** 新增或修改某天的工时：{ workCenterId（0 为全厂）, calDate, hours, remark } */
export const saveCalendar = (data) => request.post('/mes/capacity/calendar', data)
export const deleteCalendar = (id) => request.delete(`/mes/capacity/calendar/${id}`)
