package com.lemon.erp.modules.scm.service;

import com.lemon.erp.framework.rule.BizRule;
import com.lemon.erp.framework.rule.BizRuleProvider;
import com.lemon.erp.modules.scm.model.CreditControl;
import org.springframework.stereotype.Component;

import java.util.List;

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

    @Override
    public List<BizRule> rules() {
        return List.of(
                BizRule.number(PURCHASE_OVER_RATE, MODULE, "scm.label.purchaseOverReceiptRate", 0, 0, 100,
                        "scm.label.ratioWhichPurchaseReceiptQuantityMay"),
                BizRule.number(SALES_OVER_RATE, MODULE, "scm.label.salesOverDeliveryRate", 0, 0, 100,
                        "scm.label.ratioWhichSalesDeliveryQuantityMay"),
                BizRule.select(CREDIT_CONTROL, MODULE, "scm.label.customerCreditLimitControl", CreditControl.ALL, CreditControl.class,
                        "scm.label.customersCreditLimitWhetherApprovingSales"));
    }
}
