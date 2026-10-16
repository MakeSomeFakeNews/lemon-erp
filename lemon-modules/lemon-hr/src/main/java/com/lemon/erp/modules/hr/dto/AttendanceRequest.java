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
        @NotNull(message = "请选择员工") Long employeeId,
        @NotNull(message = "请选择日期") LocalDate workDate,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime checkIn,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime checkOut,
        @NotNull(message = "请填写补录说明") @Size(min = 1, max = 255, message = "补录说明为1-255个字符") String remark) {
}
