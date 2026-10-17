package com.lemon.erp.modules.hr.entity;

import com.lemon.erp.modules.hr.model.TrainingStatus;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 参训记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_training_record")
public class HrTrainingRecord extends BaseEntity {

    /** 培训 */
    private Long trainingId;

    /** 学员 */
    private Long employeeId;

    /** 是否出勤 1是 0否 */
    private Integer attended;

    /** 考核成绩 */
    private BigDecimal score;

    /** 是否通过 1是 0否 */
    private Integer passed;

    /** 评价 */
    private String feedback;

    @TableField(exist = false)
    private String empNo;
    @TableField(exist = false)
    private String employeeName;
    @TableField(exist = false)
    private String deptName;
    @TableField(exist = false)
    private String trainingTitle;
    @TableField(exist = false)
    private LocalDate startDate;
    @TableField(exist = false)
    private LocalDate endDate;
    @TableField(exist = false)
    private BigDecimal hours;
    @TableField(exist = false)
    private String category;
    @TableField(exist = false)
    private TrainingStatus trainingStatus;
}
