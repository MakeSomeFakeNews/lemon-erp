package com.lemon.erp.modules.pm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 工时
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_timesheet")
public class PmTimesheet extends BaseEntity {

    /** 项目 */
    private Long projectId;

    /** 任务 */
    private Long taskId;

    /** 填报人 */
    private Long userId;

    /** 工作日期 */
    private LocalDate workDate;

    /** 工时 */
    private BigDecimal hours;

    /** 工作内容 */
    private String content;

    /** 状态(字典 pm_timesheet_status) SUBMITTED待审批 APPROVED已通过 REJECTED已驳回 */
    private String status;

    /** 审批时的小时成本 */
    private BigDecimal costRate;

    /** 人工成本 = 工时 × 小时成本（审批通过后） */
    private BigDecimal cost;

    /** 审批人 */
    private Long approveBy;

    /** 审批时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approveTime;

    /** 驳回原因 */
    private String rejectReason;

    /** 项目月结凭证（已入总账） */
    private Long voucherId;

    @TableField(exist = false)
    private String projectCode;
    @TableField(exist = false)
    private String projectName;
    @TableField(exist = false)
    private String taskName;
    @TableField(exist = false)
    private String userName;
    @TableField(exist = false)
    private String approveByName;
}
