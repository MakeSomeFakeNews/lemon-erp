package com.lemon.erp.modules.fin.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * @param amount   核销金额（实收实付），红字（退货、冲账）单据为负数；有折让时可以为 0
 * @param discount 折让 / 抹零金额，与单据金额同号，可为空；核销金额 + 折让不超过未核销金额，单据的已核销同时累加折让
 */
public record SettlementItemRequest(
        @NotNull(message = "{fin.valid.selectDocument}") Long billId,
        @NotNull(message = "{fin.valid.enterWriteOffAmount}") @Digits(integer = 16, fraction = 2, message = "{fin.valid.amountAllowsMostDecimalPlaces}") BigDecimal amount,
        @Digits(integer = 16, fraction = 2, message = "{fin.valid.amountAllowsMostDecimalPlaces}") BigDecimal discount) {

    public SettlementItemRequest(Long billId, BigDecimal amount) {
        this(billId, amount, null);
    }

    /** 折让金额，没有为 0 */
    public BigDecimal discountOrZero() {
        return discount == null ? BigDecimal.ZERO : discount;
    }
}
