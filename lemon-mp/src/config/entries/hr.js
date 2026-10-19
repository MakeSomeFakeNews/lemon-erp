/** 工作台入口（见 index.js）：员工自助，登录即可访问 */
export default [
  { key: 'hr-clock', group: 'office', title: 'hr.entry.clock', color: '#13c2c2', url: '/pkg-hr/clock/index', links: ['/hr/self'] },
  { key: 'hr-attendance', group: 'office', title: 'hr.entry.attendance', color: '#2f54eb', url: '/pkg-hr/attendance/index' },
  { key: 'hr-leave', group: 'office', title: 'hr.entry.leave', color: '#fa8c16', url: '/pkg-hr/leave/index' },
  { key: 'hr-payslip', group: 'office', title: 'hr.entry.payslip', color: '#52c41a', url: '/pkg-hr/payslip/index' },
  { key: 'hr-appraisal', group: 'office', title: 'hr.entry.appraisal', color: '#722ed1', url: '/pkg-hr/appraisal/index' }
]
