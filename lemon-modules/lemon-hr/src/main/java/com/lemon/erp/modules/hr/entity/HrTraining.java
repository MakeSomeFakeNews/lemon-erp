package com.lemon.erp.modules.hr.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.modules.hr.model.TrainingStatus;
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

    @NotBlank(message = "{hr.valid.trainingTopicCannotEmpty}")
    @Size(max = 128, message = "{hr.valid.trainingTopicCannotExceedCharacters}")
    private String title;

    /** 类别(字典 hr_training_category) */
    private String category;

    @Size(max = 64, message = "{hr.valid.trainerCannotExceedCharacters}")
    private String trainer;

    @NotNull(message = "{hr.valid.selectStartDate}")
    private LocalDate startDate;

    @NotNull(message = "{hr.valid.selectEndDate}")
    private LocalDate endDate;

    /** 课时（小时） */
    @DecimalMin(value = "0", message = "{hr.valid.hoursCannotLess}")
    @Digits(integer = 6, fraction = 1, message = "{hr.valid.hoursHaveMostDecimalPlace}")
    private BigDecimal hours;

    @Size(max = 128, message = "{hr.valid.locationCannotExceedCharacters}")
    private String location;

    @DecimalMin(value = "0", message = "{hr.valid.costCannotLess}")
    @Digits(integer = 16, fraction = 2, message = "{hr.valid.costHaveMostDecimalPlaces}")
    private BigDecimal cost;

    /** 培训费凭证（培训结束时生成） */
    private Long voucherId;

    /** PLANNED / ONGOING / FINISHED / CANCELED */
    private TrainingStatus status;

    @Size(max = 1000, message = "{hr.valid.trainingContentCannotExceedCharacters}")
    private String content;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    /** 学员人数、出勤人数、通过人数 */
    @TableField(exist = false)
    private Integer traineeCount;
    @TableField(exist = false)
    private Integer attendedCount;
    @TableField(exist = false)
    private Integer passedCount;
}
