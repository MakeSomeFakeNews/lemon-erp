package com.lemon.erp.modules.hr.dto;

import com.lemon.erp.modules.hr.model.LeaveKind;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 请假 / 加班申请
 *
 * @param kind  LEAVE 请假 / OVERTIME 加班
 * @param type  假期类型（字典 hr_leave_type）；加班时为补偿方式（OvertimeComp），不填为加班费
 * @param hours 时长（小时，0.5 的倍数）
 */
public record LeaveRequest(
        @NotNull(message = "{hr.valid.selectRequestKind}") LeaveKind kind,
        String type,
        @NotNull(message = "{hr.valid.selectStartTime}") @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
        @NotNull(message = "{hr.valid.selectEndTime}") @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
        @NotNull(message = "{hr.valid.enterDuration}") @DecimalMin(value = "0.5", message = "{hr.valid.durationMustLeastHours}")
        @DecimalMax(value = "2000", message = "{hr.valid.durationCannotExceedHours}") @Digits(integer = 5, fraction = 1, message = "{hr.valid.durationHaveMostDecimalPlace}") BigDecimal hours,
        @Size(max = 255, message = "{hr.valid.reasonCannotExceedCharacters}") String reason) {
}
