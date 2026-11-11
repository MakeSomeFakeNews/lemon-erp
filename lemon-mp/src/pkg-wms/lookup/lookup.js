import { OrderKind, QuickPrice } from '@web/constants/enums'
import { hasPerm } from '@/store/user'

/**
 * 商品速查的公共逻辑。前端只用于决定显示哪些分段 / 菜单，价格是否返回由后端按同样的规则判断
 */

/** 能否查看售价 / 进价历史（与后端 ScmPricingService.canViewSales / canViewPurchase 一致） */
export function canViewKind(kind) {
  return kind === OrderKind.PURCHASE
    ? hasPerm(['scm:purchase:list', 'scm:purchaseIn:list'])
    : hasPerm(['scm:sales:list', 'scm:salesOut:list', 'scm:quotation:list'])
}

/** 点击价格时打开哪种历史：进货类价格（参考进货价、最新进价、库存均价）看进价历史，其他看售价历史 */
export function kindOfPrice(type) {
  return [QuickPrice.REF_COST, QuickPrice.LAST_PURCHASE, QuickPrice.AVG_COST].includes(type) ? OrderKind.PURCHASE : OrderKind.SALES
}
