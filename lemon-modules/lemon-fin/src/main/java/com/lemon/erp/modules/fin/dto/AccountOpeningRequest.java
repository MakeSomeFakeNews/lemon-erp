package com.lemon.erp.modules.fin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 科目期初余额（整体替换），借方合计必须等于贷方合计
 */
public record AccountOpeningRequest(
        @NotNull(message = "{fin.valid.enterOpeningBalances}") @Size(max = 2000, message = "{fin.valid.accountsCannotExceed}") List<@Valid @NotNull Item> items) {

    public record Item(
            @NotNull(message = "{fin.valid.selectAccount}") Long accountId,
            @DecimalMin(value = "0", message = "{fin.valid.debitAmountCannotLess}") @Digits(integer = 16, fraction = 2, message = "{fin.valid.amountAllowsMostDecimalPlaces}") BigDecimal debit,
            @DecimalMin(value = "0", message = "{fin.valid.creditAmountCannotLess}") @Digits(integer = 16, fraction = 2, message = "{fin.valid.amountAllowsMostDecimalPlaces}") BigDecimal credit) {
    }
}
