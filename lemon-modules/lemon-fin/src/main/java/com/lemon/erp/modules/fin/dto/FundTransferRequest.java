package com.lemon.erp.modules.fin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 账户调拨
 *
 * @param fee 手续费，由转出账户承担
 */
public record FundTransferRequest(
        @NotNull(message = "{fin.valid.selectTransferDate}") LocalDate transferDate,
        @NotNull(message = "{fin.valid.selectSourceAccount}") Long fromAccountId,
        @NotNull(message = "{fin.valid.selectDestinationAccount}") Long toAccountId,
        @NotNull(message = "{fin.valid.enterAmount}") @DecimalMin(value = "0", inclusive = false, message = "{fin.valid.amountMustGreater2}")
        @Digits(integer = 16, fraction = 2, message = "{fin.valid.amountAllowsMostDecimalPlaces}") BigDecimal amount,
        @DecimalMin(value = "0", message = "{fin.valid.feeCannotLess}") @Digits(integer = 16, fraction = 2, message = "{fin.valid.amountAllowsMostDecimalPlaces}") BigDecimal fee,
        @Size(max = 255, message = "{fin.valid.remarkCannotExceedCharacters}") String remark) {
}
