package com.lemon.erp.modules.pm.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 新增 / 修改任务。上级任务的日期、工时、进度由下级汇总
 *
 * @param parentId      上级任务，顶级为 0 或空
 * @param predecessorId 前置任务（完成 - 开始），前置任务完成前不能开始
 */
public record TaskRequest(
        @NotNull(message = "{pm.valid.selectProject}") Long projectId,
        Long parentId,
        @NotBlank(message = "{pm.valid.taskNameRequired}") @Size(max = 128, message = "{pm.valid.taskNameCannotExceedCharacters}") String name,
        Long assigneeId,
        @NotNull(message = "{pm.valid.selectPlannedStartDate}") LocalDate planStart,
        @NotNull(message = "{pm.valid.selectPlannedEndDate}") LocalDate planEnd,
        @DecimalMin(value = "0", message = "{pm.valid.plannedHoursCannotNegative}") @Digits(integer = 7, fraction = 1, message = "{pm.valid.hoursAllowMostDecimalPlace}") BigDecimal planHours,
        Integer milestone,
        Long predecessorId,
        Integer sort,
        @Size(max = 500, message = "{pm.valid.descriptionCannotExceedCharacters2}") String description) {
}
