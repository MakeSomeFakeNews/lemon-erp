package com.lemon.erp.modules.hr.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 工资表按部门汇总：人数、应发、实发、个人社保公积金与个税、单位社保公积金
 */
@Data
public class PayrollDeptRow {

    private Long deptId;
    private String deptName;
    private Long headcount;
    private BigDecimal grossPay;
    private BigDecimal netPay;
    private BigDecimal personalDeduct;
    private BigDecimal incomeTax;
    private BigDecimal companyCost;
}
