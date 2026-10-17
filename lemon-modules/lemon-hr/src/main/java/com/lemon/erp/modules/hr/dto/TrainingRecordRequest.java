package com.lemon.erp.modules.hr.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 培训出勤与成绩
 */
public record TrainingRecordRequest(
        @NotNull(message = "{hr.valid.attendanceRecordsCannotEmpty}") Long id,
        Integer attended,
        @DecimalMin(value = "0", message = "{hr.valid.scoreMustBetween2}") @DecimalMax(value = "100", message = "{hr.valid.scoreMustBetween2}") BigDecimal score,
        Integer passed,
        @Size(max = 255, message = "{hr.valid.feedbackCannotExceedCharacters}") String feedback) {
}
