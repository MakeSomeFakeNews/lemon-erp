package com.lemon.erp.modules.pm.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 收款计划（按里程碑）
 */
public record BillingRequest(
        @NotNull(message = "请选择项目") Long projectId,
        @NotBlank(message = "请填写款项名称") @Size(max = 64, message = "款项名称不能超过64个字符") String name,
        Long taskId,
        @NotNull(message = "请选择计划开票日期") LocalDate planDate,
        @NotNull(message = "请填写金额") @DecimalMin(value = "0.01", message = "金额必须大于0") @Digits(integer = 16, fraction = 2, message = "金额最多2位小数") BigDecimal amount,
        @Size(max = 255, message = "备注不能超过255个字符") String remark) {
}
