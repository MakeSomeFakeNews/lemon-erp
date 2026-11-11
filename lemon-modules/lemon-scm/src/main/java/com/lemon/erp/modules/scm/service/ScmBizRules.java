package com.lemon.erp.modules.scm.service;

import com.lemon.erp.framework.rule.BizRule;
import com.lemon.erp.framework.rule.BizRuleProvider;
import com.lemon.erp.modules.scm.model.CreditControl;
import com.lemon.erp.modules.scm.model.PurchaseFreightCost;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 供应链的业务参数（模块、名称、说明为消息键）
 */
@Component
public class ScmBizRules implements BizRuleProvider {

    private static final String MODULE = "scm.label.supplyChain";

    /** 采购入库允许超过订单数量的比例（%） */
    public static final String PURCHASE_OVER_RATE = "scm.purchaseIn.overRate";
    /** 销售出库允许超过订单数量的比例（%） */
    public static final String SALES_OVER_RATE = "scm.salesOut.overRate";
    /** 信用额度控制方式 */
    public static final String CREDIT_CONTROL = "scm.credit.control";
    /** 销售出库、采购入库（及退货）可以不选订单直接开单，系统生成隐式订单（来源 DIRECT） */
    public static final String DIRECT_DOC = "scm.directDoc.enabled";
    /** 销售出库的明细行可以各自选择仓库 */
    public static final String SALES_OUT_LINE_WAREHOUSE = "scm.salesOut.lineWarehouse";
    /** 本公司承担的采购运费计入存货成本或当期费用（供应商代垫的始终计入存货成本） */
    public static final String PURCHASE_FREIGHT_COST = "scm.purchaseFreight.cost";
    /** 采购入库 / 退货、销售出库 / 退货先存草稿，审核后才出入库、生成应收应付（默认关闭：保存即审核，与原来一致） */
    public static final String TRADE_DRAFT = "scm.tradeDoc.draft";
    /** 默认税率（%）：新建单据明细、价格资料没有其他税率来源（价格资料、发票类型）时使用 */
    public static final String DEFAULT_TAX_RATE = "scm.defaultTaxRate";

    @Override
    public List<BizRule> rules() {
        return List.of(
                BizRule.number(PURCHASE_OVER_RATE, MODULE, "scm.label.purchaseOverReceiptRate", 0, 0, 100,
                        "scm.label.ratioWhichPurchaseReceiptQuantityMay"),
                BizRule.number(SALES_OVER_RATE, MODULE, "scm.label.salesOverDeliveryRate", 0, 0, 100,
                        "scm.label.ratioWhichSalesDeliveryQuantityMay"),
                BizRule.select(CREDIT_CONTROL, MODULE, "scm.label.customerCreditLimitControl", CreditControl.ALL, CreditControl.class,
                        "scm.label.customersCreditLimitWhetherApprovingSales"),
                BizRule.bool(DIRECT_DOC, MODULE, "scm.label.directDocEnabled", true, "scm.label.directDocEnabledRemark"),
                BizRule.bool(SALES_OUT_LINE_WAREHOUSE, MODULE, "scm.label.salesOutLineWarehouse", true,
                        "scm.label.salesOutLineWarehouseRemark"),
                BizRule.select(PURCHASE_FREIGHT_COST, MODULE, "scm.label.purchaseFreightCost", PurchaseFreightCost.INVENTORY,
                        PurchaseFreightCost.class, "scm.label.purchaseFreightCostRemark"),
                BizRule.bool(TRADE_DRAFT, MODULE, "scm.label.tradeDraft", false, "scm.label.tradeDraftRemark"),
                BizRule.decimal(DEFAULT_TAX_RATE, MODULE, "scm.label.defaultTaxRate", "13", 2, 0, 100, "scm.label.defaultTaxRateRemark"));
    }

    @Override
    public Set<String> publicKeys() {
        return Set.of(DIRECT_DOC, SALES_OUT_LINE_WAREHOUSE, PURCHASE_FREIGHT_COST, TRADE_DRAFT, DEFAULT_TAX_RATE);
    }
}
