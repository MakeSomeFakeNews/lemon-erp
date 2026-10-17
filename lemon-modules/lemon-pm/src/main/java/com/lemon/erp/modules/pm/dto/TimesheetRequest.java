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
        @NotNull(message = "{pm.valid.selectProject}") Long projectId,
        Long taskId,
        @NotNull(message = "{pm.valid.selectDate}") LocalDate workDate,
        @NotNull(message = "{pm.valid.enterHours}") @DecimalMin(value = "0.5", message = "{pm.valid.hoursMustLeast}") @DecimalMax(value = "24", message = "{pm.valid.hoursCannotExceed}")
        @Digits(integer = 2, fraction = 1, message = "{pm.valid.hoursAllowMostDecimalPlace}") BigDecimal hours,
        @Size(max = 255, message = "{pm.valid.workContentCannotExceedCharacters}") String content) {
}
