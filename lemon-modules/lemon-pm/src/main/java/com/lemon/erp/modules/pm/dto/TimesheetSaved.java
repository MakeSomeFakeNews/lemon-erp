package com.lemon.erp.modules.pm.dto;

/**
 * 工时保存结果
 *
 * @param warning 提示（如当天填报的工时超过出勤时长），为空表示没有
 */
public record TimesheetSaved(Long id, String warning) {
}
