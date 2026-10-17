package com.lemon.erp.modules.fin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 员工借款（草稿）
 *
 * @param userId 借款人，为空时为当前用户
 */
public record LoanRequest(
        Long userId,
        Long deptId,
        @NotNull(message = "{fin.valid.selectLoanDate}") LocalDate loanDate,
        @NotNull(message = "{fin.valid.enterAmount}") @DecimalMin(value = "0.01", message = "{fin.valid.amountMustGreater}")
        @Digits(integer = 16, fraction = 2, message = "{fin.valid.amountAllowsMostDecimalPlaces}") BigDecimal amount,
        @Size(max = 255, message = "{fin.valid.reasonCannotExceedCharacters}") String purpose) {
}
