package com.lemon.erp.modules.fin.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * @param amount 核销金额，红字（退货）单据为负数
 */
public record SettlementItemRequest(
        @NotNull(message = "{fin.valid.selectDocument}") Long billId,
        @NotNull(message = "{fin.valid.enterWriteOffAmount}") @Digits(integer = 16, fraction = 2, message = "{fin.valid.amountAllowsMostDecimalPlaces}") BigDecimal amount) {
}
