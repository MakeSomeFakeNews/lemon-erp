package com.lemon.erp.modules.pm.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 收款计划（按里程碑）
 */
public record BillingRequest(
        @NotNull(message = "{pm.valid.selectProject}") Long projectId,
        @NotBlank(message = "{pm.valid.enterPaymentName}") @Size(max = 64, message = "{pm.valid.paymentNameCannotExceedCharacters}") String name,
        Long taskId,
        @NotNull(message = "{pm.valid.selectPlannedInvoiceDate}") LocalDate planDate,
        @NotNull(message = "{pm.valid.enterAmount}") @DecimalMin(value = "0.01", message = "{pm.valid.amountMustGreater}") @Digits(integer = 16, fraction = 2, message = "{pm.valid.amountAllowsMostDecimalPlaces}") BigDecimal amount,
        @Size(max = 255, message = "{pm.valid.remarkCannotExceedCharacters}") String remark) {
}
