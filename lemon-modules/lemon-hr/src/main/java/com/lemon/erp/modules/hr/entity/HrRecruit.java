package com.lemon.erp.modules.hr.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.modules.hr.model.RecruitStatus;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 招聘需求
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_recruit")
public class HrRecruit extends BaseEntity {

    /** 需求单号 */
    private String recruitNo;

    /** 招聘岗位名称 */
    private String title;

    /** 用人部门 */
    private Long deptId;

    /** 职位 */
    private Long positionId;

    /** 招聘人数 */
    private Integer headcount;

    /** 已录用人数 */
    private Integer hiredCount;

    /** 薪资范围 */
    private String salaryRange;

    /** 任职要求 */
    private String requirement;

    /** 招聘原因 */
    private String reason;

    /** 期望到岗日期 */
    private LocalDate deadline;

    /** 状态(字典 hr_recruit_status) DRAFT草稿 APPROVING审批中 OPEN招聘中 CLOSED已关闭 */
    private RecruitStatus status;

    /** 审批流程实例 */
    private String flowInstanceId;

    /** 申请人 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String deptName;
    @TableField(exist = false)
    private String positionName;
    @TableField(exist = false)
    private String createByName;
    /** 应聘者人数 */
    @TableField(exist = false)
    private Integer candidateCount;
}
