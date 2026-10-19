/** 工作台入口（见 index.js）：扫码作业与查询，权限与 Web 端移动作业、对应接口一致 */
export default [
  {
    key: 'wms-stock',
    group: 'warehouse',
    title: 'wms.entry.stock',
    color: '#13c2c2',
    url: '/pkg-wms/stock/index',
    perm: 'wms:stock:list',
    links: ['/wms/stock']
  },
  {
    key: 'wms-receive',
    group: 'warehouse',
    title: 'wms.entry.receive',
    color: '#1677ff',
    url: '/pkg-wms/receive/index',
    perm: 'scm:purchaseIn:add',
    links: ['/m/receive', '/scm/purchase-in']
  },
  {
    key: 'wms-putaway',
    group: 'warehouse',
    title: 'wms.entry.putaway',
    color: '#722ed1',
    url: '/pkg-wms/putaway/index',
    perm: 'wms:transfer:add',
    links: ['/m/putaway']
  },
  {
    key: 'wms-pick',
    group: 'warehouse',
    title: 'wms.entry.pick',
    color: '#fa8c16',
    url: '/pkg-wms/pick/index',
    perm: 'scm:delivery:pick',
    links: ['/m/pick', '/scm/delivery']
  },
  {
    key: 'wms-check',
    group: 'warehouse',
    title: 'wms.entry.check',
    color: '#52c41a',
    url: '/pkg-wms/check/index',
    perm: 'wms:check:edit',
    links: ['/m/check', '/wms/check']
  },
  {
    key: 'mes-report',
    group: 'warehouse',
    title: 'wms.entry.report',
    color: '#eb2f96',
    url: '/pkg-wms/report/index',
    perm: 'mes:wo:report',
    // 报工页要查询工单列表和详情
    permAll: ['mes:wo:list'],
    links: ['/mes/wo']
  }
]
