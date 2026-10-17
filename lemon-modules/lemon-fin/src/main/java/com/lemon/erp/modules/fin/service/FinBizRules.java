package com.lemon.erp.modules.fin.service;

import com.lemon.erp.framework.rule.BizRule;
import com.lemon.erp.framework.rule.BizRuleProvider;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 财务的业务参数
 */
@Component
public class FinBizRules implements BizRuleProvider {

    private static final String MODULE = "fin.label.finance";

    /** 允许制单人审核自己的凭证 */
    public static final String VOUCHER_SELF_AUDIT = "fin.voucher.selfAudit";

    @Override
    public List<BizRule> rules() {
        return List.of(
                BizRule.bool(VOUCHER_SELF_AUDIT, MODULE, "fin.label.allowApprovingVouchersPreparedOneself", false,
                        "fin.label.whenOffSegregationDutiesAppliesPreparer"));
    }
}
