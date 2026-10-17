package com.lemon.erp.modules.fin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 凭证分录：借方、贷方只能填一边
 *
 * 辅助核算（部门、项目、客户、供应商、存货）可同时填写，科目上设置了的必填
 *
 * @param accountId 末级科目
 */
public record VoucherEntryRequest(
        @NotBlank(message = "{fin.valid.enterSummary}") @Size(max = 255, message = "{fin.valid.summaryCannotExceedCharacters}") String summary,
        @NotNull(message = "{fin.valid.selectAccount}") Long accountId,
        Long deptId,
        @DecimalMin(value = "0", message = "{fin.valid.amountCannotLess}") @Digits(integer = 16, fraction = 2, message = "{fin.valid.amountAllowsMostDecimalPlaces}") BigDecimal debit,
        @DecimalMin(value = "0", message = "{fin.valid.amountCannotLess}") @Digits(integer = 16, fraction = 2, message = "{fin.valid.amountAllowsMostDecimalPlaces}") BigDecimal credit,
        Long projectId,
        Long customerId,
        Long supplierId,
        Long materialId) {
}
