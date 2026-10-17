package com.lemon.erp.modules.hr.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 批量录入培训出勤与成绩
 */
public record TrainingRecordsRequest(@NotNull(message = "{hr.valid.enterAttendanceRecords}") List<@Valid @NotNull TrainingRecordRequest> items) {
}
