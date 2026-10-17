package com.lemon.erp.modules.fin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 费用报销（草稿）
 *
 * @param userId 报销人，为空时为当前用户
 * @param deptId 费用部门，为空时取报销人的部门
 * @param loanId 付款时冲抵的借款（选填）
 */
public record ClaimRequest(
        Long userId,
        Long deptId,
        @NotNull(message = "{fin.valid.selectClaimDate}") LocalDate claimDate,
        Long loanId,
        @Size(max = 255, message = "{fin.valid.reasonCannotExceedCharacters}") String remark,
        @NotEmpty(message = "{fin.valid.addExpenseClaimDetails}") @Size(max = 100, message = "{fin.valid.detailsCannotExceedLines}") List<@Valid @NotNull Item> items) {

    public record Item(
            LocalDate expenseDate,
            @NotNull(message = "{fin.valid.selectExpenseAccount}") Long accountId,
            Long projectId,
            @NotNull(message = "{fin.valid.enterAmount}") @DecimalMin(value = "0.01", message = "{fin.valid.amountMustGreater}")
            @Digits(integer = 16, fraction = 2, message = "{fin.valid.amountAllowsMostDecimalPlaces}") BigDecimal amount,
            @Size(max = 255, message = "{fin.valid.descriptionCannotExceedCharacters}") String remark) {
    }
}
