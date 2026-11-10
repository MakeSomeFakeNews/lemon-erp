package com.lemon.erp.modules.fin.service;

import com.lemon.erp.framework.rule.BizRule;
import com.lemon.erp.framework.rule.BizRuleProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 财务的业务参数
 */
@Component
public class FinBizRules implements BizRuleProvider {

    private static final String MODULE = "fin.label.finance";

    /** 允许制单人审核自己的凭证 */
    public static final String VOUCHER_SELF_AUDIT = "fin.voucher.selfAudit";

    /** 冲账单复核人不能是制单人（默认关闭，小公司一个人操作） */
    public static final String OFFSET_REVIEW_SEPARATE = "fin.offset.reviewSeparate";

    /** 收付款先存草稿，确认后才核销（默认关闭：保存即生效，与原来一致） */
    public static final String SETTLEMENT_DRAFT = "fin.settlement.draft";

    @Override
    public List<BizRule> rules() {
        return List.of(
                BizRule.bool(VOUCHER_SELF_AUDIT, MODULE, "fin.label.allowApprovingVouchersPreparedOneself", false,
                        "fin.label.whenOffSegregationDutiesAppliesPreparer"),
                BizRule.bool(OFFSET_REVIEW_SEPARATE, MODULE, "fin.label.offsetReviewSeparate", false,
                        "fin.label.offsetReviewSeparateHint"),
                BizRule.bool(SETTLEMENT_DRAFT, MODULE, "fin.label.settlementDraft", false,
                        "fin.label.settlementDraftHint"));
    }

    /** 收付款页面按是否存草稿显示「保存草稿」「确认」按钮 */
    @Override
    public Set<String> publicKeys() {
        return Set.of(SETTLEMENT_DRAFT);
    }
}
