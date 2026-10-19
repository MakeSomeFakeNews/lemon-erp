import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

/** 职位 */
export const pagePosition = (params) => request.get('/hr/position', { params })
export const positionOptions = () => request.get('/hr/position/options')
export const addPosition = (data) => request.post('/hr/position', data)
export const updatePosition = (id, data) => request.put(`/hr/position/${id}`, data)
export const changePositionStatus = (id, status) => request.put(`/hr/position/${id}/status`, { status })
export const deletePosition = (id) => request.delete(`/hr/position/${ids(id)}`)

/** 员工档案与人事异动 */
export const pageEmployee = (params) => request.get('/hr/employee', { params })
export const employeeStats = () => request.get('/hr/employee/stats')
export const employeeOptions = (keyword) => request.get('/hr/employee/options', { params: { keyword } })
export const getEmployee = (id) => request.get(`/hr/employee/${id}`)
export const employeeChanges = (id) => request.get(`/hr/employee/${id}/changes`)
export const employeeTrainings = (id) => request.get(`/hr/employee/${id}/trainings`)
export const addEmployee = (data) => request.post('/hr/employee', data)
export const updateEmployee = (id, data) => request.put(`/hr/employee/${id}`, data)
export const changeEmployee = (id, data) => request.post(`/hr/employee/${id}/change`, data)
export const deleteEmployee = (id) => request.delete(`/hr/employee/${ids(id)}`)

/** 招聘 */
export const pageRecruit = (params) => request.get('/hr/recruit', { params })
export const openRecruits = () => request.get('/hr/recruit/open')
export const getRecruit = (id) => request.get(`/hr/recruit/${id}`)
export const addRecruit = (data) => request.post('/hr/recruit', data)
export const updateRecruit = (id, data) => request.put(`/hr/recruit/${id}`, data)
export const deleteRecruit = (id) => request.delete(`/hr/recruit/${ids(id)}`)
export const approveRecruit = (id) => request.post(`/hr/recruit/${id}/approve`)
export const withdrawRecruit = (id) => request.post(`/hr/recruit/${id}/withdraw`)
export const closeRecruit = (id) => request.post(`/hr/recruit/${id}/close`)
export const pageCandidate = (params) => request.get('/hr/candidate', { params })
export const getCandidate = (id) => request.get(`/hr/candidate/${id}`)
export const addCandidate = (data) => request.post('/hr/candidate', data)
export const updateCandidate = (id, data) => request.put(`/hr/candidate/${id}`, data)
export const deleteCandidate = (id) => request.delete(`/hr/candidate/${ids(id)}`)
export const candidateInterviews = (id) => request.get(`/hr/candidate/${id}/interviews`)
export const addInterview = (id, data) => request.post(`/hr/candidate/${id}/interview`, data)
export const updateInterview = (id, data) => request.put(`/hr/interview/${id}`, data)
export const offerCandidate = (id) => request.post(`/hr/candidate/${id}/offer`)
export const rejectCandidate = (id, reason) => request.post(`/hr/candidate/${id}/reject`, { reason })
export const hireCandidate = (id, data) => request.post(`/hr/candidate/${id}/hire`, data)

/** 培训 */
export const pageTraining = (params) => request.get('/hr/training', { params })
export const getTraining = (id) => request.get(`/hr/training/${id}`)
export const trainingRecords = (id) => request.get(`/hr/training/${id}/records`)
export const addTraining = (data) => request.post('/hr/training', data)
export const updateTraining = (id, data) => request.put(`/hr/training/${id}`, data)
export const deleteTraining = (id) => request.delete(`/hr/training/${ids(id)}`)
export const changeTrainingStatus = (id, status) => request.put(`/hr/training/${id}/status`, { status })
export const addTrainees = (id, employeeIds) => request.post(`/hr/training/${id}/trainees`, employeeIds)
export const removeTrainees = (id, recordIds) => request.delete(`/hr/training/${id}/trainees/${ids(recordIds)}`)
export const saveTrainingRecords = (id, items) => request.put(`/hr/training/${id}/records`, { items })

/** 考勤、班次、节假日 */
export const pageAttendance = (params) => request.get('/hr/attendance', { params })
export const attendanceSummary = (params) => request.get('/hr/attendance/summary', { params })
export const saveAttendance = (data) => request.post('/hr/attendance', data)
export const deleteAttendance = (id) => request.delete(`/hr/attendance/${ids(id)}`)
export const listShift = () => request.get('/hr/shift')
export const addShift = (data) => request.post('/hr/shift', data)
export const updateShift = (id, data) => request.put(`/hr/shift/${id}`, data)
export const deleteShift = (id) => request.delete(`/hr/shift/${id}`)
export const listHoliday = (year) => request.get('/hr/holiday', { params: { year } })
export const addHoliday = (data) => request.post('/hr/holiday', data)
export const updateHoliday = (id, data) => request.put(`/hr/holiday/${id}`, data)
export const deleteHoliday = (id) => request.delete(`/hr/holiday/${ids(id)}`)

/** 假勤审批 */
export const pageLeave = (params) => request.get('/hr/leave', { params })
export const approveLeave = (id, data) => request.post(`/hr/leave/${id}/approve`, data)

/** 薪资档案、薪酬核算 */
export const pageSalary = (params) => request.get('/hr/salary', { params })
export const getSalary = (employeeId) => request.get(`/hr/salary/${employeeId}`)
export const saveSalary = (employeeId, data) => request.put(`/hr/salary/${employeeId}`, data)
export const salaryHistory = (employeeId) => request.get(`/hr/salary/${employeeId}/history`)
export const salaryRules = () => request.get('/hr/salary/rules')
export const pagePayroll = (params) => request.get('/hr/payroll', { params })
export const getPayroll = (id, keyword) => request.get(`/hr/payroll/${id}`, { params: { keyword } })
export const generatePayroll = (period) => request.post('/hr/payroll/generate', { period })
export const adjustPayrollItem = (itemId, data) => request.put(`/hr/payroll/item/${itemId}`, data)
export const confirmPayroll = (id) => request.post(`/hr/payroll/${id}/confirm`)
export const unconfirmPayroll = (id) => request.post(`/hr/payroll/${id}/unconfirm`)
export const accruePayroll = (id) => request.post(`/hr/payroll/${id}/accrual`)
export const payPayroll = (id, data) => request.post(`/hr/payroll/${id}/pay`, data)
export const deletePayroll = (id) => request.delete(`/hr/payroll/${id}`)

/** 社保方案（不分页） */
export const listSocialScheme = () => request.get('/hr/social-scheme')
export const addSocialScheme = (data) => request.post('/hr/social-scheme', data)
export const updateSocialScheme = (id, data) => request.put(`/hr/social-scheme/${id}`, data)
export const deleteSocialScheme = (id) => request.delete(`/hr/social-scheme/${id}`)

/** 专项附加扣除 */
export const pageTaxDeduction = (params) => request.get('/hr/tax-deduction', { params })
export const addTaxDeduction = (data) => request.post('/hr/tax-deduction', data)
export const updateTaxDeduction = (id, data) => request.put(`/hr/tax-deduction/${id}`, data)
export const deleteTaxDeduction = (id) => request.delete(`/hr/tax-deduction/${ids(id)}`)

/** 劳动合同 */
export const pageContract = (params) => request.get('/hr/contract', { params })
export const getContract = (id) => request.get(`/hr/contract/${id}`)
export const addContract = (data) => request.post('/hr/contract', data)
export const updateContract = (id, data) => request.put(`/hr/contract/${id}`, data)
export const deleteContract = (id) => request.delete(`/hr/contract/${id}`)
/** 续签，返回新合同 ID */
export const renewContract = (id, data) => request.post(`/hr/contract/${id}/renew`, data)
export const terminateContract = (id, date, reason) => request.post(`/hr/contract/${id}/terminate`, null, { params: { date, reason } })

/** 绩效考核 */
export const pagePerf = (params) => request.get('/hr/perf', { params })
export const getPerf = (id) => request.get(`/hr/perf/${id}`)
export const perfGrades = (id) => request.get(`/hr/perf/${id}/grades`)
export const addPerf = (data) => request.post('/hr/perf', data)
export const updatePerf = (id, data) => request.put(`/hr/perf/${id}`, data)
export const deletePerf = (id) => request.delete(`/hr/perf/${id}`)
export const startPerf = (id, deptIds) => request.post(`/hr/perf/${id}/start`, { deptIds })
export const finishPerf = (id) => request.post(`/hr/perf/${id}/finish`)
export const pageAppraisal = (params) => request.get('/hr/perf/appraisal', { params })
export const getAppraisal = (id) => request.get(`/hr/perf/appraisal/${id}`)
export const reviewAppraisal = (id, data) => request.post(`/hr/perf/appraisal/${id}/review`, data)
export const reopenAppraisal = (id) => request.post(`/hr/perf/appraisal/${id}/reopen`)

/** 员工自助 */
export const selfProfile = () => request.get('/hr/self/profile')
export const selfClock = () => request.post('/hr/self/clock')
export const selfCalendar = (period) => request.get('/hr/self/calendar', { params: { period } })
export const selfSummary = (period) => request.get('/hr/self/summary', { params: { period } })
/** 请假估算规则 { dayHours, lunchThreshold, lunchBreak }（当前租户的业务参数） */
export const leaveRules = () => request.get('/hr/self/leave-rules')
export const selfLeaves = () => request.get('/hr/self/leave')
export const applyLeave = (data) => request.post('/hr/self/leave', data)
export const cancelLeave = (id) => request.post(`/hr/self/leave/${id}/cancel`)
export const selfPayslips = () => request.get('/hr/self/payslips')
export const selfTrainings = () => request.get('/hr/self/trainings')
export const selfAppraisals = (params) => request.get('/hr/self/appraisals', { params })
export const selfReviews = (params) => request.get('/hr/self/reviews', { params })
export const selfEvaluate = (id, data) => request.post(`/hr/self/appraisal/${id}/self`, data)
/** Excel 导入员工档案：{ rows: [{ rowNum, data }] } */
export const importEmployee = (data) => request.post('/hr/employee/import', data)
