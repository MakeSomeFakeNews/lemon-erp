package com.lemon.erp.modules.fin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 收付款中冲抵的预收 / 预付款
 *
 * @param advanceId 预收 / 预付单
 * @param amount    冲抵金额（外币为原币），不超过其未冲抵余额
 */
public record SettlementAdvanceRequest(
        @NotNull(message = "{fin.valid.selectAdvance}") Long advanceId,
        @NotNull(message = "{fin.valid.enterOffsetAmount}")
        @DecimalMin(value = "0", inclusive = false, message = "{fin.valid.offsetAmountMustPositive}")
        @Digits(integer = 16, fraction = 2, message = "{fin.valid.amountAllowsMostDecimalPlaces}") BigDecimal amount) {
}
