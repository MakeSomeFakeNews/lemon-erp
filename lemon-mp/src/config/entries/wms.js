/** 工作台入口（见 index.js）：扫码作业与查询，权限与 Web 端移动作业、对应接口一致 */
export default [
  {
    // 快速开单：销售出库直接开单（不引用订单），参数关闭时不显示
    key: 'wms-quick-sale',
    group: 'sales',
    title: 'wms.entry.quickSale',
    color: '#f5222d',
    url: '/pkg-wms/sale/index',
    perm: 'scm:salesOut:add',
    rule: 'scm.directDoc.enabled'
  },
  {
    // 商品速查：名称、规格、简拼搜索商品，看库存和按设置显示的价格（价格按权限由后端过滤），点击联查图片、库存分布、价格历史
    key: 'scm-quick-lookup',
    group: 'sales',
    title: 'lookup.entry',
    color: '#fa541c',
    url: '/pkg-wms/lookup/index',
    perm: 'scm:quickLookup:list',
    links: ['/scm/quick-lookup']
  },
  {
    // 快速入库：采购入库直接开单（不引用采购订单），参数关闭时不显示
    key: 'wms-quick-purchase',
    group: 'warehouse',
    title: 'trade.entry.purchase',
    color: '#2f54eb',
    url: '/pkg-wms/purchase/index',
    perm: 'scm:purchaseIn:add',
    rule: 'scm.directDoc.enabled'
  },
  {
    // 单据草稿：销售出库、采购入库的草稿，查看、审核记账、删除；参数「进销存单据先存草稿」关闭时不显示
    key: 'scm-trade-draft',
    group: 'sales',
    title: 'trade.entry.draft',
    color: '#faad14',
    url: '/pkg-wms/draft/index',
    perm: ['scm:salesOut:list', 'scm:purchaseIn:list'],
    rule: 'scm.tradeDoc.draft'
  },
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
