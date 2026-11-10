import { t } from '@/locales'
import {
  addPurchaseRebate, addRebate, calcPurchaseRebate, calcRebate, deletePurchaseRebate, deleteRebate, getPurchaseRebate, getRebate,
  pagePurchaseRebate, pageRebate, settlePurchaseRebate, settleRebate, unsettlePurchaseRebate, unsettleRebate, updatePurchaseRebate, updateRebate
} from '@/api/scm/sourcing'
import { RebateKind } from '@/constants/enums'

/** 返利协议的接口：销售返利 /scm/rebate，采购返利 /scm/purchase-rebate */
export function rebateApi(kind) {
  return kind === RebateKind.PURCHASE
    ? { page: pagePurchaseRebate, get: getPurchaseRebate, calc: calcPurchaseRebate, add: addPurchaseRebate, update: updatePurchaseRebate,
        remove: deletePurchaseRebate, settle: settlePurchaseRebate, unsettle: unsettlePurchaseRebate }
    : { page: pageRebate, get: getRebate, calc: calcRebate, add: addRebate, update: updateRebate,
        remove: deleteRebate, settle: settleRebate, unsettle: unsettleRebate }
}

/** 销售返利 / 采购返利措辞不同的文字（客户 ↔ 供应商、销售额 ↔ 采购额、红字应收 ↔ 红字应付） */
const TEXTS = {
  searchPlaceholder: ['scm.rebate.searchPlaceholder', 'scm.rebate.purchaseSearchPlaceholder'],
  add: ['scm.rebate.add', 'scm.rebate.purchaseAdd'],
  edit: ['scm.rebate.edit', 'scm.rebate.purchaseEdit'],
  timesRate: ['scm.rebate.salesTimesRate', 'scm.rebate.purchaseTimesRate'],
  creditNoteNo: ['scm.rebate.creditNoteNo', 'scm.rebate.debitNoteNo'],
  creditNote: ['scm.rebate.creditNote', 'scm.rebate.debitNote'],
  tiersTip: ['scm.rebate.tiersTip', 'scm.rebate.purchaseTiersTip'],
  reach: ['scm.rebate.salesReach', 'scm.rebate.purchaseReach'],
  detailTitle: ['scm.rebate.detailTitle', 'scm.rebate.purchaseDetailTitle'],
  unsettleConfirm: ['scm.rebate.unsettleConfirm', 'scm.rebate.purchaseUnsettleConfirm'],
  settleConfirm: ['scm.rebate.settleConfirm', 'scm.rebate.purchaseSettleConfirm'],
  nextTierText: ['scm.rebate.nextTierText', 'scm.rebate.purchaseNextTierText'],
  baseAmount: ['scm.rebate.salesAmount', 'scm.rebate.purchaseAmount']
}

/** 返回 (名称, 参数) => 文字，按返利种类取对应的语言包键 */
export function useRebateText(kind) {
  const i = kind === RebateKind.PURCHASE ? 1 : 0
  return (name, params) => t(TEXTS[name][i], params || {})
}
