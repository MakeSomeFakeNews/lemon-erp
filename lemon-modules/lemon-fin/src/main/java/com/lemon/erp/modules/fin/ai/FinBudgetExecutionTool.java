package com.lemon.erp.modules.fin.ai;

import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.framework.ai.AiTool;
import com.lemon.erp.framework.ai.AiToolArgs;
import com.lemon.erp.framework.ai.AiToolResult;
import com.lemon.erp.modules.fin.dto.BudgetExecution;
import com.lemon.erp.modules.fin.service.FinBudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 预算执行：年度预算各科目（部门）的全年预算、截至本月的累计预算与执行数、执行率，复用 {@link FinBudgetService#execution}
 */
@Component
@RequiredArgsConstructor
public class FinBudgetExecutionTool implements AiTool {

    /**
     * 一个预算项目，金额单位元
     *
     * @param ytdBudget      截至本月（往年为全年）的累计预算
     * @param ytdActual      截至本月的累计执行数
     * @param ytdRatePercent 累计执行率（%）
     * @param ratePercent    全年执行率（%）
     */
    record BudgetItem(String deptName, String accountCode, String accountName, BigDecimal annualBudget, BigDecimal annualActual,
                      BigDecimal ratePercent, BigDecimal ytdBudget, BigDecimal ytdActual, BigDecimal ytdRatePercent) {
    }

    private final FinBudgetService budgetService;

    @Override
    public String name() {
        return "fin_budget_execution";
    }

    @Override
    public String labelKey() {
        return "fin.ai.tool.budgetExecution";
    }

    @Override
    public String descriptionKey() {
        return "fin.ai.tool.budgetExecution.desc";
    }

    @Override
    public Map<String, Object> parameters() {
        AiSchema s = AiSchema.object().prop("year", AiSchema.integer("fin.ai.param.year").min(2000).max(2099), false);
        return FinAiSupport.keyword(s, "fin.ai.param.budgetKeyword").build();
    }

    @Override
    public String permission() {
        return "fin:budget:list";
    }

    @Override
    public Object execute(AiToolArgs args) {
        LocalDate today = LocalDate.now();
        int year = args.getInt("year", today.getYear());
        int months = year < today.getYear() ? 12 : year > today.getYear() ? 0 : today.getMonthValue();
        String keyword = args.getString(FinAiSupport.KEYWORD);
        List<BudgetItem> rows = budgetService.execution(year, null).stream()
                .filter(e -> FinAiSupport.matches(keyword, e.deptName(), e.accountCode(), e.accountName()))
                .map(e -> {
                    BigDecimal budget = BigDecimal.ZERO;
                    BigDecimal actual = BigDecimal.ZERO;
                    for (int i = 0; i < months; i++) {
                        budget = budget.add(Decimals.nz(e.budgets().get(i)));
                        actual = actual.add(Decimals.nz(e.actuals().get(i)));
                    }
                    return new BudgetItem(e.deptName(), e.accountCode(), e.accountName(), e.totalBudget(), e.totalActual(), e.rate(),
                            budget, actual, Decimals.percent(actual, budget, 1));
                })
                .sorted(Comparator.comparing(BudgetItem::ytdRatePercent, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        return AiToolResult.rows(rows);
    }
}
