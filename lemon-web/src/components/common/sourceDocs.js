import { BillKind, BillSource } from '@/constants/enums'

/**
 * 来源单据的打开方式（SourceDocLink 使用），按应收应付单的来源类型（BillSource）登记：
 * - trade：在当前页面打开出入库单详情（TradeDocDetail 的 kind）
 * - ticket / project：在当前页面打开服务工单 / 项目详情（项目开票按应收单上的项目打开）
 * - path：跳转到对应页面并打开详情（页面用 useRouteOpen 处理 ?id）；byNo 为 true 时按单号筛选（?keyword=单号，页面没有详情抽屉）
 * perm 为查看权限，没有权限时只显示单号。新增来源类型时在这里登记
 *
 * @type {Record<string, { perm: string | ((doc) => string), trade?: string, ticket?: boolean, project?: boolean,
 *   path?: string | ((doc) => string), byNo?: boolean }>}
 */
export const SOURCE_DOCS = {
  [BillSource.SALES_OUT]: { perm: 'scm:salesOut:list', trade: 'salesOut' },
  [BillSource.SALES_RETURN]: { perm: 'scm:salesReturn:list', trade: 'salesReturn' },
  [BillSource.PURCHASE_IN]: { perm: 'scm:purchaseIn:list', trade: 'purchaseIn' },
  [BillSource.PURCHASE_RETURN]: { perm: 'scm:purchaseReturn:list', trade: 'purchaseReturn' },
  // 采购运费：来源为采购入库单
  [BillSource.PURCHASE_FREIGHT]: { perm: 'scm:purchaseIn:list', trade: 'purchaseIn' },
  [BillSource.TICKET]: { perm: 'crm:ticket:list', ticket: true },
  [BillSource.PROJECT]: { perm: 'pm:project:list', project: true },
  [BillSource.SHIPMENT]: { perm: 'scm:shipment:list', path: '/scm/shipment' },
  // 订单尾差调整：来源为订单
  [BillSource.ORDER_ADJUST]: {
    perm: (d) => (d.billKind === BillKind.AP ? 'scm:purchase:list' : 'scm:sales:list'),
    path: (d) => (d.billKind === BillKind.AP ? '/scm/purchase' : '/scm/sales')
  },
  [BillSource.REBATE]: {
    perm: (d) => (d.billKind === BillKind.AP ? 'scm:purchaseRebate:list' : 'scm:rebate:list'),
    path: (d) => (d.billKind === BillKind.AP ? '/scm/purchase-rebate' : '/scm/rebate')
  },
  [BillSource.OFFSET]: { perm: 'fin:offset:list', path: '/fin/offset' },
  [BillSource.NOTE]: { perm: 'fin:note:list', path: '/fin/note' },
  [BillSource.QUALITY_CLAIM]: { perm: 'qms:claim:list', path: '/qms/claim', byNo: true },
  [BillSource.ASSET]: { perm: 'fin:asset:list', path: '/fin/asset', byNo: true }
}

const resolve = (v, doc) => (typeof v === 'function' ? v(doc) : v)

/**
 * 来源单据的打开方式，不能打开（期初 / 手工、没有来源 ID、没有权限）时返回 null
 *
 * @param {{ type: string, id?: number, no?: string, billKind?: string, projectId?: number }} doc
 * @param {(perm: string) => boolean} hasPerm
 */
export function sourceTarget(doc, hasPerm) {
  const conf = SOURCE_DOCS[doc.type]
  if (!conf) return null
  if (conf.project ? !doc.projectId : conf.byNo ? !doc.no : !doc.id) return null
  if (!hasPerm(resolve(conf.perm, doc))) return null
  return { ...conf, path: resolve(conf.path, doc) }
}
