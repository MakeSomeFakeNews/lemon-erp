/** 工作台入口：销售外勤（见 index.js） */
export default [
  {
    key: 'crm-customer',
    group: 'sales',
    title: 'crm.entry.customer',
    color: '#1677ff',
    url: '/pkg-crm/customer/index',
    perm: 'crm:customer:list'
  },
  {
    key: 'crm-followup',
    group: 'sales',
    title: 'crm.entry.followup',
    color: '#13c2c2',
    url: '/pkg-crm/followup/index',
    perm: ['crm:followup:list', 'crm:customer:list'],
    // 「今日待跟进客户」的待办、跟进提醒和公海回收的消息链接到客户列表，在小程序中进入跟进页（今日待跟进）
    links: ['/crm/customer', '/crm/followup']
  },
  {
    key: 'crm-opportunity',
    group: 'sales',
    title: 'crm.entry.opportunity',
    color: '#722ed1',
    url: '/pkg-crm/opportunity/index',
    perm: 'crm:opp:list',
    links: ['/crm/opportunity']
  },
  {
    key: 'crm-order',
    group: 'sales',
    title: 'crm.entry.order',
    color: '#fa8c16',
    url: '/pkg-crm/order/index',
    perm: 'scm:sales:list',
    links: ['/scm/sales']
  },
  {
    key: 'crm-lead',
    group: 'sales',
    title: 'crm.entry.lead',
    color: '#faad14',
    url: '/pkg-crm/lead/index',
    perm: 'crm:lead:list',
    links: ['/crm/lead']
  },
  {
    key: 'crm-ticket',
    group: 'sales',
    title: 'crm.entry.ticket',
    color: '#eb2f96',
    url: '/pkg-crm/ticket/index',
    perm: 'crm:ticket:list',
    // 工单超时提醒等消息链接到 /crm/ticket
    links: ['/crm/ticket']
  }
]
