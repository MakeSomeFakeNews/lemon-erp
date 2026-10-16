package com.lemon.erp.modules.fin.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 预算执行：各月预算数与执行数（已记账凭证，不含结转损益），费用类取借方 - 贷方，收入类取贷方 - 借方
 *
 * @param budgets 1-12 月预算
 * @param actuals 1-12 月执行数
 * @param rate    全年执行率（%），预算为 0 时为空
 */
public record BudgetExecution(Long budgetId, Integer budgetYear, Long deptId, String deptName, Long accountId,
                              String accountCode, String accountName, List<BigDecimal> budgets, List<BigDecimal> actuals,
                              BigDecimal totalBudget, BigDecimal totalActual, BigDecimal rate) {
}
