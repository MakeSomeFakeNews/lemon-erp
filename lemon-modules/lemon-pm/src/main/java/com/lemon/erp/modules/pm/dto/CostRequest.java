package com.lemon.erp.modules.pm.dto;

import com.lemon.erp.modules.pm.model.CostType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 录入项目费用（人工成本来自工时，不在此录入）
 */
public record CostRequest(
        @NotNull(message = "{pm.valid.selectProject}") Long projectId,
        @NotNull(message = "{pm.valid.selectCostType}") CostType costType,
        @NotNull(message = "{pm.valid.enterAmount}") @DecimalMin(value = "0.01", message = "{pm.valid.amountMustGreater}") @Digits(integer = 16, fraction = 2, message = "{pm.valid.amountAllowsMostDecimalPlaces}") BigDecimal amount,
        @NotNull(message = "{pm.valid.selectDate}") LocalDate costDate,
        @Size(max = 255, message = "{pm.valid.descriptionCannotExceedCharacters}") String description) {
}
