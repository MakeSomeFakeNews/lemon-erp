package com.lemon.erp.modules.pm.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 录入项目费用（人工成本来自工时，不在此录入）
 */
public record CostRequest(
        @NotNull(message = "请选择项目") Long projectId,
        @NotBlank(message = "请选择成本类型") String costType,
        @NotNull(message = "请填写金额") @DecimalMin(value = "0.01", message = "金额必须大于0") @Digits(integer = 16, fraction = 2, message = "金额最多2位小数") BigDecimal amount,
        @NotNull(message = "请选择日期") LocalDate costDate,
        @Size(max = 255, message = "说明不能超过255个字符") String description) {
}
