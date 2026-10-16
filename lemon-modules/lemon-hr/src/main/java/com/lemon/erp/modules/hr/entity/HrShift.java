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

    @NotBlank(message = "班次名称不能为空")
    @Size(max = 64, message = "班次名称不能超过64个字符")
    private String name;

    @NotNull(message = "请选择上班时间")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @NotNull(message = "请选择下班时间")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    /** 迟到 / 早退宽限（分钟） */
    @Min(value = 0, message = "宽限分钟不能小于0")
    @Max(value = 120, message = "宽限分钟不能超过120")
    private Integer flexMinutes;

    /** 标准工时（小时），请假按此折算天数 */
    @DecimalMin(value = "1", message = "标准工时至少1小时")
    @DecimalMax(value = "12", message = "标准工时不能超过12小时")
    private BigDecimal workHours;

    /** 是否默认班次，未指定班次的员工使用 */
    private Integer isDefault;

    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;
}
