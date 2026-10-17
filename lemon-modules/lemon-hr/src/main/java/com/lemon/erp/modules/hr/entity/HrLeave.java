package com.lemon.erp.modules.hr.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.modules.hr.model.LeaveKind;
import com.lemon.erp.modules.hr.model.LeaveStatus;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 请假 / 加班申请
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_leave")
public class HrLeave extends BaseEntity {

    /** 申请单号 */
    private String leaveNo;

    /** 员工 */
    private Long employeeId;

    /** 种类 LEAVE请假 OVERTIME加班 */
    private LeaveKind kind;

    /** 假期类型(字典 hr_leave_type)，加班为空 */
    private String type;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /** 时长（小时） */
    private BigDecimal hours;

    /** 事由 */
    private String reason;

    /** 状态(字典 hr_leave_status) APPROVING审批中 APPROVED已通过 REJECTED已拒绝 CANCELED已撤销 */
    private LeaveStatus status;

    /** 审批流程实例，为空时由人事审批 */
    private String flowInstanceId;

    /** 审批人 */
    private Long approveBy;

    /** 审批时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approveTime;

    /** 审批意见 */
    private String approveComment;

    /** 员工部门（数据权限） */
    private Long deptId;

    /** 申请人 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String empNo;
    @TableField(exist = false)
    private String employeeName;
    @TableField(exist = false)
    private String deptName;
    @TableField(exist = false)
    private String approveByName;
}
