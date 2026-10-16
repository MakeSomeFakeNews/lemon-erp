package com.lemon.erp.modules.fin.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 手工录入应收 / 应付单（期初余额等）
 *
 * @param kind    AR / AP
 * @param dueDate 到期日，不填按往来单位账期计算
 */
public record BillRequest(
        @NotBlank(message = "请选择种类") String kind,
        @NotNull(message = "请选择往来单位") Long partnerId,
        @NotNull(message = "请选择日期") LocalDate billDate,
        LocalDate dueDate,
        @NotNull(message = "请填写金额") @Digits(integer = 16, fraction = 2, message = "金额最多2位小数") BigDecimal amount,
        @Size(max = 255, message = "备注不能超过255个字符") String remark) {
}
