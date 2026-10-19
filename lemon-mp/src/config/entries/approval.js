/** 工作台入口（见 index.js）：审批、发起申请 */
export default [
  {
    key: 'approval',
    group: 'office',
    title: 'approval.entry',
    color: '#1677ff',
    url: '/pages/approval/index',
    tab: true,
    perm: ['wf:task:todo', 'wf:task:done', 'wf:instance:mine', 'wf:copy:list'],
    links: ['/workflow/todo', '/workflow/done', '/workflow/mine', '/workflow/copy']
  },
  {
    key: 'approval-start',
    group: 'office',
    title: 'approval.start.entry',
    color: '#13c2c2',
    url: '/pkg-approval/start/index',
    perm: 'wf:start:list',
    links: ['/workflow/start']
  }
]
