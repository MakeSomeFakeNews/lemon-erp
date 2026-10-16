package com.lemon.erp.modules.hr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 薪资历史：每次建立、调整薪资档案按生效日期记录一条；工资表按月末生效的薪资计算，补算历史月份也正确
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_salary_history")
public class HrSalaryHistory extends BaseEntity {

    private Long employeeId;

    private LocalDate effectiveDate;

    private BigDecimal baseSalary;

    private BigDecimal postAllowance;

    private BigDecimal otherAllowance;

    private BigDecimal perfBonusBase;

    private BigDecimal socialBase;

    private BigDecimal housingBase;

    private BigDecimal housingRate;

    private Long schemeId;

    private String remark;
}
