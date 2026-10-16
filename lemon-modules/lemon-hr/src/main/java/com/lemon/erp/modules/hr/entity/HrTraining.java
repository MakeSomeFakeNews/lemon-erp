package com.lemon.erp.modules.hr.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 培训计划：计划中 → 进行中 → 已结束（录入出勤和成绩），也可以取消
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_training")
public class HrTraining extends BaseEntity {

    @NotBlank(message = "培训主题不能为空")
    @Size(max = 128, message = "培训主题不能超过128个字符")
    private String title;

    /** 类别(字典 hr_training_category) */
    private String category;

    @Size(max = 64, message = "讲师不能超过64个字符")
    private String trainer;

    @NotNull(message = "请选择开始日期")
    private LocalDate startDate;

    @NotNull(message = "请选择结束日期")
    private LocalDate endDate;

    /** 课时（小时） */
    @DecimalMin(value = "0", message = "课时不能小于0")
    @Digits(integer = 6, fraction = 1, message = "课时最多1位小数")
    private BigDecimal hours;

    @Size(max = 128, message = "地点不能超过128个字符")
    private String location;

    @DecimalMin(value = "0", message = "费用不能小于0")
    @Digits(integer = 16, fraction = 2, message = "费用最多2位小数")
    private BigDecimal cost;

    /** 培训费凭证（培训结束时生成） */
    private Long voucherId;

    /** PLANNED / ONGOING / FINISHED / CANCELED */
    private String status;

    @Size(max = 1000, message = "培训内容不能超过1000个字符")
    private String content;

    private Long createBy;

    /** 学员人数、出勤人数、通过人数 */
    @TableField(exist = false)
    private Integer traineeCount;
    @TableField(exist = false)
    private Integer attendedCount;
    @TableField(exist = false)
    private Integer passedCount;
}
