import { OrderKind, QuickPrice } from '@/constants/enums'

/** 点击价格时打开哪种历史：进货类价格（参考进货价、最新进价、库存均价）看进价历史，其他看售价历史 */
export function kindOfPrice(type) {
  return [QuickPrice.REF_COST, QuickPrice.LAST_PURCHASE, QuickPrice.AVG_COST].includes(type) ? OrderKind.PURCHASE : OrderKind.SALES
}
