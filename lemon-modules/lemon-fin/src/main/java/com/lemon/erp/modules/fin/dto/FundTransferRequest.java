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
        @NotNull(message = "请选择调拨日期") LocalDate transferDate,
        @NotNull(message = "请选择转出账户") Long fromAccountId,
        @NotNull(message = "请选择转入账户") Long toAccountId,
        @NotNull(message = "请填写金额") @DecimalMin(value = "0", inclusive = false, message = "金额必须大于 0")
        @Digits(integer = 16, fraction = 2, message = "金额最多2位小数") BigDecimal amount,
        @DecimalMin(value = "0", message = "手续费不能小于 0") @Digits(integer = 16, fraction = 2, message = "金额最多2位小数") BigDecimal fee,
        @Size(max = 255, message = "备注不能超过255个字符") String remark) {
}
