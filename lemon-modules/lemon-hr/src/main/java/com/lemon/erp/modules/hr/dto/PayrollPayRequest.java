package com.lemon.erp.modules.hr.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * 发放工资
 *
 * @param accountId 付款账户（库存现金 / 银行存款下的末级科目）
 */
public record PayrollPayRequest(@NotNull(message = "{hr.valid.selectPaymentDate}") LocalDate payDate,
                                @NotNull(message = "{hr.valid.selectPaymentAccount}") Long accountId) {
}
