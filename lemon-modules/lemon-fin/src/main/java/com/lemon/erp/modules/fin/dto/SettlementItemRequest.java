package com.lemon.erp.modules.fin.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * @param amount 核销金额，红字（退货）单据为负数
 */
public record SettlementItemRequest(
        @NotNull(message = "请选择单据") Long billId,
        @NotNull(message = "请填写核销金额") @Digits(integer = 16, fraction = 2, message = "金额最多2位小数") BigDecimal amount) {
}
