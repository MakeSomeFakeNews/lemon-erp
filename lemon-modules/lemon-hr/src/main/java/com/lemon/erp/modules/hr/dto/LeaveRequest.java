package com.lemon.erp.modules.hr.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 请假 / 加班申请
 *
 * @param kind  LEAVE 请假 / OVERTIME 加班
 * @param type  假期类型（字典 hr_leave_type），加班不填
 * @param hours 时长（小时，0.5 的倍数）
 */
public record LeaveRequest(
        @NotNull(message = "请选择申请种类") @Pattern(regexp = "LEAVE|OVERTIME", message = "申请种类不正确") String kind,
        String type,
        @NotNull(message = "请选择开始时间") @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
        @NotNull(message = "请选择结束时间") @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
        @NotNull(message = "请填写时长") @DecimalMin(value = "0.5", message = "时长至少0.5小时")
        @DecimalMax(value = "2000", message = "时长不能超过2000小时") @Digits(integer = 5, fraction = 1, message = "时长最多1位小数") BigDecimal hours,
        @Size(max = 255, message = "事由不能超过255个字符") String reason) {
}
