package com.lemon.erp.modules.hr.entity;

import com.lemon.erp.framework.workflow.ApproveStatus;
import com.lemon.erp.modules.hr.model.ChangeType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 人事异动
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_employee_change")
public class HrEmployeeChange extends BaseEntity {

    /** 员工 */
    private Long employeeId;

    /** 异动类型(字典 hr_change_type) ENTRY入职 REGULAR转正 TRANSFER调岗 SALARY调薪 LEAVE离职 */
    private ChangeType type;

    /** 生效日期 */
    private LocalDate changeDate;

    /** 原部门 */
    private Long fromDeptId;

    /** 新部门 */
    private Long toDeptId;

    /** 原职位 */
    private Long fromPositionId;

    /** 新职位 */
    private Long toPositionId;

    /** 原基本工资 */
    private BigDecimal fromSalary;

    /** 新基本工资 */
    private BigDecimal toSalary;

    /** 说明 */
    private String reason;

    /** DONE 已生效 / APPROVING 审批中 / REJECTED 已拒绝 / CANCELED 已撤回 */
    private ApproveStatus status;

    private String flowInstanceId;

    /** 离职交接人（审批通过后移交） */
    private Long handoverUserId;

    /** 办理人 */
    private Long createBy;

    @TableField(exist = false)
    private String fromDeptName;
    @TableField(exist = false)
    private String toDeptName;
    @TableField(exist = false)
    private String fromPositionName;
    @TableField(exist = false)
    private String toPositionName;
    @TableField(exist = false)
    private String createByName;
}
