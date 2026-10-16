package com.lemon.erp.modules.fin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 员工借款（草稿）
 *
 * @param userId 借款人，为空时为当前用户
 */
public record LoanRequest(
        Long userId,
        Long deptId,
        @NotNull(message = "请选择借款日期") LocalDate loanDate,
        @NotNull(message = "请填写金额") @DecimalMin(value = "0.01", message = "金额必须大于0")
        @Digits(integer = 16, fraction = 2, message = "金额最多2位小数") BigDecimal amount,
        @Size(max = 255, message = "事由不能超过255个字符") String purpose) {
}
