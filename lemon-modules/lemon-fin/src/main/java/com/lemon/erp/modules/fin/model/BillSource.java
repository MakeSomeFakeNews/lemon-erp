package com.lemon.erp.modules.fin.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * 应收应付来源：应收应付单的来源（字典 fin_bill_source）
 */
public enum BillSource {
    /** 销售出库 */
    SALES_OUT,
    /** 销售退货 */
    SALES_RETURN,
    /** 采购入库 */
    PURCHASE_IN,
    /** 采购退货 */
    PURCHASE_RETURN,
    /** 期初 / 手工 */
    MANUAL,
    /** 项目开票 */
    PROJECT,
    /** 运费 */
    SHIPMENT,
    /** 服务收费 */
    TICKET,
    /** 固定资产 */
    ASSET,
    /** 票据拒付 */
    NOTE,
    /** 质量索赔 */
    QUALITY_CLAIM,
    /** 销售返利 */
    REBATE;

    /** 供应链出入库生成的单据（与 scm TradeKind 同名） */
    public static final Set<BillSource> TRADE = EnumSet.of(SALES_OUT, SALES_RETURN, PURCHASE_IN, PURCHASE_RETURN);

    /** 是否由供应链出入库生成 */
    public boolean isTrade() {
        return TRADE.contains(this);
    }
}
