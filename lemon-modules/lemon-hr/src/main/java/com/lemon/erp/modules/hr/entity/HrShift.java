package com.lemon.erp.modules.hr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 班次：上班打卡晚于 上班时间 + 宽限 为迟到，下班打卡早于 下班时间 - 宽限 为早退
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_shift")
public class HrShift extends BaseEntity {

    @NotBlank(message = "{hr.valid.shiftNameCannotEmpty}")
    @Size(max = 64, message = "{hr.valid.shiftNameCannotExceedCharacters}")
    private String name;

    @NotNull(message = "{hr.valid.selectStartTime2}")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @NotNull(message = "{hr.valid.selectEndTime2}")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    /** 迟到 / 早退宽限（分钟） */
    @Min(value = 0, message = "{hr.valid.graceMinutesCannotLess}")
    @Max(value = 120, message = "{hr.valid.graceMinutesCannotExceed}")
    private Integer flexMinutes;

    /** 标准工时（小时），请假按此折算天数 */
    @DecimalMin(value = "1", message = "{hr.valid.standardHoursMustLeastHour}")
    @DecimalMax(value = "12", message = "{hr.valid.standardHoursCannotExceedHours}")
    private BigDecimal workHours;

    /** 是否默认班次，未指定班次的员工使用 */
    private Integer isDefault;

    @Size(max = 255, message = "{hr.valid.remarkCannotExceedCharacters}")
    private String remark;
}
