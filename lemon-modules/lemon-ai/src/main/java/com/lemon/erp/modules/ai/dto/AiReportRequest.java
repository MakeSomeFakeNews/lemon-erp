package com.lemon.erp.modules.ai.dto;

import com.lemon.erp.modules.ai.model.AiReportPeriodType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * 手动生成报告
 *
 * @param periodType 周报 / 月报
 * @param date       期间内的任一天（周报为所在周，月报为所在月）
 */
public record AiReportRequest(@NotNull(message = "{ai.valid.reportPeriodTypeRequired}") AiReportPeriodType periodType,
                              @NotNull(message = "{ai.valid.reportDateRequired}") LocalDate date) {
}
