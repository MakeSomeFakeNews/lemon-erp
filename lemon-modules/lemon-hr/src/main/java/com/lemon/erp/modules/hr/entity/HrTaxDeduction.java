package com.lemon.erp.modules.hr.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 个税专项附加扣除：每月扣除金额，在起止月份内计入累计预扣
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_tax_deduction")
public class HrTaxDeduction extends BaseEntity {

    private Long employeeId;

    /** 字典 hr_deduction_type */
    private String type;

    private BigDecimal monthlyAmount;

    /** yyyyMM */
    private Integer startPeriod;

    private Integer endPeriod;

    private String remark;

    @TableField(exist = false)
    private String empNo;
    @TableField(exist = false)
    private String employeeName;
    @TableField(exist = false)
    private String deptName;
}
