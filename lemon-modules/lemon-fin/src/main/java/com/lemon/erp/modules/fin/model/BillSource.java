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
    REBATE,
    /** 冲账单（垫付运费、返利、三包赔偿等抵扣往来款，红字） */
    OFFSET,
    /** 订单尾差调整：部分出入库的订单关闭时，已出入库金额与订单口径的差额（蓝字或红字） */
    ORDER_ADJUST,
    /** 采购运费：采购入库的运费（供应商代垫或付承运商），计入存货成本 */
    PURCHASE_FREIGHT,
    /** 销售运费：销售出库单的运费，来源为该出库单（月结的承运商应付、代垫运费的客户应收） */
    SALES_FREIGHT,
    /** 销售退货运费：客户代垫的退货运费，来源为该退货单（红字应收） */
    SALES_RETURN_FREIGHT,
    /** 采购退货运费：本公司替供应商垫付的退货运费，来源为该退货单（红字应付、月结的承运商应付） */
    PURCHASE_RETURN_FREIGHT;

    /** 供应链出入库生成的单据（与 scm TradeKind 同名） */
    public static final Set<BillSource> TRADE = EnumSet.of(SALES_OUT, SALES_RETURN, PURCHASE_IN, PURCHASE_RETURN);

    /** 是否由供应链出入库生成 */
    public boolean isTrade() {
        return TRADE.contains(this);
    }
}
