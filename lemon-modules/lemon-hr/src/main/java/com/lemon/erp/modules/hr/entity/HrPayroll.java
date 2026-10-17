package com.lemon.erp.modules.hr.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.modules.hr.model.PayrollStatus;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工资表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_payroll")
public class HrPayroll extends BaseEntity {

    /** 工资表号 */
    private String payrollNo;

    /** 工资月份 yyyyMM */
    private Integer period;

    /** 状态(字典 hr_payroll_status) DRAFT草稿 CONFIRMED已确认 PAID已发放 */
    private PayrollStatus status;

    /** 人数 */
    private Integer headcount;

    /** 应发合计 */
    private BigDecimal grossTotal;

    /** 个人社保公积金合计 */
    private BigDecimal personalTotal;

    /** 个税合计 */
    private BigDecimal taxTotal;

    /** 实发合计 */
    private BigDecimal netTotal;

    /** 单位社保公积金合计 */
    private BigDecimal companyTotal;

    /** 发放日期 */
    private LocalDate payDate;

    /** 计提凭证 */
    private Long accrueVoucherId;

    /** 审批流程实例ID */
    private String flowInstanceId;

    /** 发放凭证 */
    private Long payVoucherId;

    /** 备注 */
    private String remark;

    /** 制表人 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    /** 确认人 */
    private Long confirmBy;

    /** 确认时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime confirmTime;

    @TableField(exist = false)
    private String createByName;
    @TableField(exist = false)
    private String confirmByName;
    @TableField(exist = false)
    private String accrueVoucherNo;
    @TableField(exist = false)
    private String payVoucherNo;
    @TableField(exist = false)
    private List<HrPayrollItem> items;
}
