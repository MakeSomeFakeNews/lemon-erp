package com.lemon.erp.modules.hr.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 考核表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_perf_appraisal")
public class HrPerfAppraisal extends BaseEntity {

    /** 考核方案 */
    private Long planId;

    /** 被考核人 */
    private Long employeeId;

    /** 部门 */
    private Long deptId;

    /** 评分人（部门负责人） */
    private Long reviewerId;

    /** 自评总分（加权） */
    private BigDecimal selfScore;

    /** 最终得分（加权） */
    private BigDecimal score;

    /** 等级(字典 hr_perf_grade) S A B C D */
    private String grade;

    /** 状态(字典 hr_appraisal_status) SELF待自评 REVIEW待评分 DONE已完成 */
    private String status;

    /** 自评总结 */
    private String selfComment;

    /** 评语 */
    private String reviewComment;

    /** 评分时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewTime;

    @TableField(exist = false)
    private String empNo;
    @TableField(exist = false)
    private String employeeName;
    @TableField(exist = false)
    private String deptName;
    @TableField(exist = false)
    private String reviewerName;
    @TableField(exist = false)
    private String planName;
    @TableField(exist = false)
    private String planStatus;
    /** 被考核人的账号 */
    @TableField(exist = false)
    private Long employeeUserId;
    @TableField(exist = false)
    private List<HrPerfScore> scores;
}
