package com.lemon.erp.modules.pm.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 填报工时（本人），必须是项目成员
 */
public record TimesheetRequest(
        @NotNull(message = "请选择项目") Long projectId,
        Long taskId,
        @NotNull(message = "请选择日期") LocalDate workDate,
        @NotNull(message = "请填写工时") @DecimalMin(value = "0.5", message = "工时至少0.5小时") @DecimalMax(value = "24", message = "工时不能超过24小时")
        @Digits(integer = 2, fraction = 1, message = "工时最多1位小数") BigDecimal hours,
        @Size(max = 255, message = "工作内容不能超过255个字符") String content) {
}
