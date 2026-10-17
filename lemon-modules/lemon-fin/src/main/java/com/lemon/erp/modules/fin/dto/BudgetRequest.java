package com.lemon.erp.modules.fin.dto;

import com.lemon.erp.modules.fin.model.BudgetControl;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 新增 / 修改预算
 *
 * @param deptId 部门，不填为全公司
 * @param months 1-12 月预算，12 个
 */
public record BudgetRequest(
        @NotNull(message = "{fin.valid.selectYear}") @Min(2000) @Max(2999) Integer budgetYear,
        Long deptId,
        @NotNull(message = "{fin.valid.selectAccount}") Long accountId,
        @NotNull(message = "{fin.valid.enterMonthlyBudgets}") @Size(min = 12, max = 12, message = "{fin.valid.enterBudgetMonths}") List<BigDecimal> months,
        @Size(max = 255) String remark,
        BudgetControl controlMode) {
}
