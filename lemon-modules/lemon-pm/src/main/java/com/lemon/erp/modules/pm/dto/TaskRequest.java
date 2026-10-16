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
        @NotNull(message = "请选择项目") Long projectId,
        Long parentId,
        @NotBlank(message = "任务名称不能为空") @Size(max = 128, message = "任务名称不能超过128个字符") String name,
        Long assigneeId,
        @NotNull(message = "请选择计划开始日期") LocalDate planStart,
        @NotNull(message = "请选择计划完成日期") LocalDate planEnd,
        @DecimalMin(value = "0", message = "计划工时不能小于0") @Digits(integer = 7, fraction = 1, message = "工时最多1位小数") BigDecimal planHours,
        Integer milestone,
        Long predecessorId,
        Integer sort,
        @Size(max = 500, message = "说明不能超过500个字符") String description) {
}
