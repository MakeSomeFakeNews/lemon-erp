package com.lemon.erp.modules.hr.dto;

import com.lemon.erp.modules.hr.model.PayrollStatus;

import java.util.List;

/**
 * 某月工资表的部门汇总（不含个人明细）
 *
 * @param period yyyyMM
 */
public record PayrollDeptSummary(int period, PayrollStatus status, List<PayrollDeptRow> rows) {
}
