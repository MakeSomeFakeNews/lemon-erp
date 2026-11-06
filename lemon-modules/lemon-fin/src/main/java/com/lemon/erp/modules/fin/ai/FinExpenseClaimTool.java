package com.lemon.erp.modules.fin.ai;

import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.framework.ai.AiTool;
import com.lemon.erp.framework.ai.AiToolArgs;
import com.lemon.erp.framework.ai.AiToolResult;
import com.lemon.erp.modules.fin.dto.ClaimSummaryRow;
import com.lemon.erp.modules.fin.service.FinClaimService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 费用报销汇总：日期范围内已审批、已付款的报销按部门或费用科目汇总，复用 {@link FinClaimService#summary}（不返回报销人明细）
 */
@Component
@RequiredArgsConstructor
public class FinExpenseClaimTool implements AiTool {

    /** 汇总维度 */
    enum GroupBy {
        DEPT, ACCOUNT
    }

    /** 一组的报销单数与金额（元）；第一行为合计（按科目汇总时一张报销单可能计入多个科目，合计行不给单数） */
    record ClaimItem(String name, Long claimCount, BigDecimal amount) {
    }

    private final FinClaimService claimService;

    @Override
    public String name() {
        return "fin_expense_claim_summary";
    }

    @Override
    public String labelKey() {
        return "fin.ai.tool.expenseClaimSummary";
    }

    @Override
    public String descriptionKey() {
        return "fin.ai.tool.expenseClaimSummary.desc";
    }

    @Override
    public Map<String, Object> parameters() {
        AiSchema s = FinAiSupport.dateRange(AiSchema.object())
                .prop("groupBy", AiSchema.enumOf("fin.ai.param.claimGroupBy", GroupBy.DEPT.name(), GroupBy.ACCOUNT.name()), false);
        return FinAiSupport.top(s).build();
    }

    @Override
    public String permission() {
        return "fin:claim:list";
    }

    @Override
    public Object execute(AiToolArgs args) {
        boolean byAccount = args.has("groupBy") && GroupBy.valueOf(args.getString("groupBy")) == GroupBy.ACCOUNT;
        List<ClaimSummaryRow> list = claimService.summary(FinAiSupport.start(args), FinAiSupport.end(args), byAccount);
        List<ClaimItem> rows = new ArrayList<>();
        BigDecimal amount = Decimals.sum(list, ClaimSummaryRow::getAmount);
        Long count = byAccount ? null : list.stream().mapToLong(r -> r.getClaimCount() == null ? 0 : r.getClaimCount()).sum();
        rows.add(new ClaimItem(FinAiSupport.totalLabel(), count, amount));
        list.stream().limit(FinAiSupport.top(args)).forEach(r -> rows.add(new ClaimItem(r.getGroupName(),
                r.getClaimCount(), Decimals.nz(r.getAmount()))));
        return AiToolResult.rows(rows, (long) list.size() + 1);
    }
}
