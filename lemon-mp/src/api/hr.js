import { get, post } from '@/utils/request'

/** 员工自助（/hr/self，登录即可访问，只操作当前账号关联的员工本人的数据） */

/** 本人档案、年假与调休余额、待自评 / 待评分数量；没有关联员工档案时 employee 为空 */
export const selfProfile = () => get('/hr/self/profile')
/** 打卡：当天第一次为上班卡，之后为下班卡，返回当天的考勤记录 */
export const selfClock = () => post('/hr/self/clock')
/** 考勤日历：period 为 yyyyMM */
export const selfCalendar = (period) => get('/hr/self/calendar', { period })
export const selfSummary = (period) => get('/hr/self/summary', { period })
/** 我的请假 / 加班申请 */
export const selfLeaves = () => get('/hr/self/leave')
/** { kind, type, startTime, endTime, hours, reason }，时间格式 yyyy-MM-dd HH:mm:ss */
export const applyLeave = (data) => post('/hr/self/leave', data)
export const cancelLeave = (id) => post(`/hr/self/leave/${id}/cancel`)
/** 已确认的工资条 */
export const selfPayslips = () => get('/hr/self/payslips')
/** 我的考核表（分页） */
export const selfAppraisals = (params) => get('/hr/self/appraisals', params)
/** 待我评分 / 我评过的考核表（分页） */
export const selfReviews = (params) => get('/hr/self/reviews', params)
/** 考核表详情（含各指标的自评、评分），本人、评分人可查看 */
export const getAppraisal = (id) => get(`/hr/perf/appraisal/${id}`)
/** 自评 / 评分：{ scores: [{ indicatorId, score }], comment } */
export const selfEvaluate = (id, data) => post(`/hr/self/appraisal/${id}/self`, data)
export const reviewAppraisal = (id, data) => post(`/hr/perf/appraisal/${id}/review`, data)
