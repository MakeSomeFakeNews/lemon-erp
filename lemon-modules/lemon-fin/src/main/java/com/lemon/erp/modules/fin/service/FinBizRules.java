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

    private static final String MODULE = "财务";

    /** 允许制单人审核自己的凭证 */
    public static final String VOUCHER_SELF_AUDIT = "fin.voucher.selfAudit";

    @Override
    public List<BizRule> rules() {
        return List.of(
                BizRule.bool(VOUCHER_SELF_AUDIT, MODULE, "允许审核自己制单的凭证", false,
                        "关闭时遵循不相容职务分离：制单人不能审核自己的凭证（超级管理员除外）"));
    }
}
