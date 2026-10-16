package com.lemon.erp.modules.hr.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * 发放工资
 *
 * @param accountId 付款账户（库存现金 / 银行存款下的末级科目）
 */
public record PayrollPayRequest(@NotNull(message = "请选择发放日期") LocalDate payDate,
                                @NotNull(message = "请选择付款账户") Long accountId) {
}
