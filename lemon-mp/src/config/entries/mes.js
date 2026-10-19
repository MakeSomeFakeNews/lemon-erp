/** 工作台入口（见 index.js）：设备与质量，权限与后端接口一致 */
export default [
  {
    key: 'mes-equipment',
    group: 'quality',
    title: 'mes.entry.equipment',
    color: '#2f54eb',
    url: '/pkg-mes/equipment/index',
    perm: 'mes:equipment:list',
    links: ['/mes/equipment']
  },
  {
    key: 'mes-maintenance',
    group: 'quality',
    title: 'mes.entry.maintenance',
    color: '#fa8c16',
    url: '/pkg-mes/maintenance/index',
    perm: 'mes:maintain:list',
    links: ['/mes/maintenance']
  },
  {
    key: 'mes-inspection',
    group: 'quality',
    title: 'mes.entry.inspection',
    color: '#52c41a',
    url: '/pkg-mes/inspection/index',
    perm: 'mes:qc:list',
    links: ['/qms/inspection']
  },
  {
    key: 'qms-ncr',
    group: 'quality',
    title: 'mes.entry.ncr',
    color: '#f5222d',
    url: '/pkg-mes/ncr/index',
    perm: 'qms:ncr:list',
    links: ['/qms/ncr']
  }
]
