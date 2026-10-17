package com.lemon.erp.modules.pm.dto;

import com.lemon.erp.modules.pm.model.CostType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * 编制项目成本预算：按成本类型，合计为项目成本预算
 */
public record BudgetRequest(@NotNull(message = "{pm.valid.enterBudget}") List<@Valid @NotNull Item> items) {

    public record Item(@NotNull(message = "{pm.valid.selectCostType}") CostType costType,
                       @NotNull(message = "{pm.valid.enterBudgetAmount}") @DecimalMin(value = "0", message = "{pm.valid.budgetCannotNegative}")
                       @Digits(integer = 16, fraction = 2, message = "{pm.valid.amountAllowsMostDecimalPlaces}") BigDecimal amount) {
    }
}
