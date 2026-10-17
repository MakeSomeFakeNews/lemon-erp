package com.lemon.erp.modules.hr.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 补录考勤（补卡）：覆盖当天的打卡时间
 */
public record AttendanceRequest(
        @NotNull(message = "{hr.valid.selectEmployee}") Long employeeId,
        @NotNull(message = "{hr.valid.selectDate}") LocalDate workDate,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime checkIn,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime checkOut,
        @NotNull(message = "{hr.valid.enterReasonManualRecord}") @Size(min = 1, max = 255, message = "{hr.valid.reasonMustCharacters}") String remark) {
}
