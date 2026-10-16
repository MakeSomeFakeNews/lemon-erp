package com.lemon.erp.modules.hr.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 工资明细
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_payroll_item")
public class HrPayrollItem extends BaseEntity {

    /** 工资表 */
    private Long payrollId;

    /** 员工 */
    private Long employeeId;

    /** 部门（计提凭证按部门辅助核算） */
    private Long deptId;

    /** 本月应出勤天数（在职期间的工作日） */
    private BigDecimal shouldDays;

    /** 本月工作日天数 */
    private BigDecimal monthDays;

    /** 旷工天数 */
    private BigDecimal absentDays;

    /** 迟到早退次数 */
    private Integer lateCount;

    /** 事假小时 */
    private BigDecimal personalHours;

    /** 病假小时 */
    private BigDecimal sickHours;

    /** 加班小时 */
    private BigDecimal overtimeHours;

    /** 基本工资（按在职天数折算） */
    private BigDecimal baseSalary;

    /** 津贴补贴 */
    private BigDecimal allowance;

    /** 加班费 */
    private BigDecimal overtimePay;

    /** 计件工资：本月报工的合格数 × 工序计件单价 */
    private BigDecimal pieceWage;

    /** 绩效等级 */
    private String perfGrade;

    /** 绩效奖金 */
    private BigDecimal perfBonus;

    /** 其他奖金（手工调整） */
    private BigDecimal otherBonus;

    /** 考勤扣款（旷工、事假、病假、迟到早退） */
    private BigDecimal attendanceDeduct;

    /** 应发工资 */
    private BigDecimal grossPay;

    /** 个人社保 */
    private BigDecimal socialPersonal;

    /** 个人公积金 */
    private BigDecimal housingPersonal;

    /** 专项附加扣除（本月） */
    private BigDecimal specialDeduct;

    /** 个人所得税 */
    private BigDecimal incomeTax;

    /** 本年累计应纳税所得额（含本月，可为负） */
    private BigDecimal cumTaxable;

    /** 本年累计已预扣个税（含本月） */
    private BigDecimal cumTax;

    /** 其他扣款（手工调整） */
    private BigDecimal otherDeduct;

    /** 实发工资 */
    private BigDecimal netPay;

    /** 单位社保 */
    private BigDecimal socialCompany;

    /** 单位公积金 */
    private BigDecimal housingCompany;

    /** 备注 */
    private String remark;

    @TableField(exist = false)
    private String empNo;
    @TableField(exist = false)
    private String employeeName;
    @TableField(exist = false)
    private String deptName;
    @TableField(exist = false)
    private Integer period;
    @TableField(exist = false)
    private String payrollStatus;
}
