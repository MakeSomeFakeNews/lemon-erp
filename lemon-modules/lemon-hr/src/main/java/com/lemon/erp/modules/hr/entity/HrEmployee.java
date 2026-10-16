package com.lemon.erp.modules.hr.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 员工档案
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_employee")
public class HrEmployee extends BaseEntity {

    /** 工号 */
    private String empNo;

    /** 姓名 */
    private String name;

    /** 性别 M男 F女 */
    private String gender;

    /** 出生日期 */
    private LocalDate birthDate;

    /** 身份证号（列表脱敏） */
    private String idCard;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 学历(字典 hr_education) */
    private String education;

    /** 部门 */
    private Long deptId;

    /** 职位 */
    private Long positionId;

    /** 关联的登录账号，员工自助使用 */
    private Long userId;

    /** 班次，为空使用默认班次 */
    private Long shiftId;

    /** 用工类型(字典 hr_emp_type) */
    private String empType;

    /** 状态(字典 hr_emp_status) PROBATION试用 REGULAR正式 LEFT离职 */
    private String status;

    /** 入职日期 */
    private LocalDate entryDate;

    /** 参加工作日期：法定年假按累计工作年限计算，为空时按入职日期 */
    private LocalDate workStartDate;

    /** 转正日期（试用期为计划转正日期） */
    private LocalDate regularDate;

    /** 离职日期 */
    private LocalDate leaveDate;

    /** 离职原因 */
    private String leaveReason;

    /** 住址 */
    private String address;

    /** 紧急联系人 */
    private String emergencyContact;

    /** 紧急联系电话 */
    private String emergencyPhone;

    /** 备注 */
    private String remark;

    @TableField(exist = false)
    private String deptName;
    @TableField(exist = false)
    private String positionName;
    /** 关联账号的用户名 */
    @TableField(exist = false)
    private String username;
    @TableField(exist = false)
    private String shiftName;
}
