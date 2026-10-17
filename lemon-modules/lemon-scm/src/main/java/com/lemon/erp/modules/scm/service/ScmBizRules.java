package com.lemon.erp.modules.scm.service;

import com.lemon.erp.framework.rule.BizRule;
import com.lemon.erp.framework.rule.BizRuleProvider;
import com.lemon.erp.modules.scm.model.CreditControl;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 供应链的业务参数
 */
@Component
public class ScmBizRules implements BizRuleProvider {

    private static final String MODULE = "供应链";

    /** 采购入库允许超过订单数量的比例（%） */
    public static final String PURCHASE_OVER_RATE = "scm.purchaseIn.overRate";
    /** 销售出库允许超过订单数量的比例（%） */
    public static final String SALES_OVER_RATE = "scm.salesOut.overRate";
    /** 信用额度控制方式 */
    public static final String CREDIT_CONTROL = "scm.credit.control";

    @Override
    public List<BizRule> rules() {
        return List.of(
                BizRule.number(PURCHASE_OVER_RATE, MODULE, "采购入库超收比例（%）", 0, 0, 100,
                        "采购入库数量可以超过订单未入库数量的比例，0 为不允许超收"),
                BizRule.number(SALES_OVER_RATE, MODULE, "销售出库超发比例（%）", 0, 0, 100,
                        "销售出库数量可以超过订单未出库数量的比例，0 为不允许超发"),
                BizRule.select(CREDIT_CONTROL, MODULE, "客户信用额度控制", CreditControl.ALL, CreditControl.class,
                        "设置了信用额度的客户：应收余额 + 未完成订单超过额度时，审核销售订单、销售出库是否禁止"));
    }
}
