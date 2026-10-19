/** 工作台入口（见 index.js）：项目工时。填报登录即可（须为项目成员），审核需要工时查看权限 */
export default [
  { key: 'pm-timesheet', group: 'office', title: 'pm.entry.log', color: '#08979c', url: '/pkg-pm/timesheet/index' },
  {
    key: 'pm-review',
    group: 'office',
    title: 'pm.entry.review',
    color: '#d48806',
    url: '/pkg-pm/review/index',
    perm: 'pm:timesheet:list',
    links: ['/pm/timesheet']
  }
]
