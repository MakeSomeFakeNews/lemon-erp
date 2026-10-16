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
        @NotNull(message = "参训记录不能为空") Long id,
        Integer attended,
        @DecimalMin(value = "0", message = "成绩为0-100") @DecimalMax(value = "100", message = "成绩为0-100") BigDecimal score,
        Integer passed,
        @Size(max = 255, message = "评价不能超过255个字符") String feedback) {
}
