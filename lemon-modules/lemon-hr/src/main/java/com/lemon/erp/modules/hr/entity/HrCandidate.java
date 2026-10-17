package com.lemon.erp.modules.hr.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.modules.hr.model.CandidateStage;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 应聘者
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_candidate")
public class HrCandidate extends BaseEntity {

    /** 招聘需求 */
    private Long recruitId;

    /** 姓名 */
    private String name;

    /** 性别 M男 F女 */
    private String gender;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 学历(字典 hr_education) */
    private String education;

    /** 毕业院校 */
    private String school;

    /** 工作年限 */
    private Integer workYears;

    /** 简历来源(字典 hr_candidate_source) */
    private String source;

    /** 期望月薪 */
    private BigDecimal expectedSalary;

    /** 阶段(字典 hr_candidate_stage) NEW新简历 INTERVIEW面试中 OFFER已发offer HIRED已入职 REJECTED已淘汰 */
    private CandidateStage stage;

    /** 简历摘要 */
    private String resume;

    /** 入职后的员工 */
    private Long employeeId;

    /** 备注 */
    private String remark;

    /** 录入人 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String recruitTitle;
    @TableField(exist = false)
    private String recruitNo;
    @TableField(exist = false)
    private Integer interviewCount;
    /** 最近一次面试结果 */
    @TableField(exist = false)
    private String lastResult;
    /** 入职工号 */
    @TableField(exist = false)
    private String empNo;
}
