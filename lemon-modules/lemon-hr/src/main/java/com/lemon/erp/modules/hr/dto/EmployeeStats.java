package com.lemon.erp.modules.hr.dto;

import java.util.List;
import java.util.Map;

/**
 * 员工概况：在职人数、试用期人数、本月入职、本月离职，按部门、学历分布
 */
public record EmployeeStats(long active, long probation, long joinedThisMonth, long leftThisMonth,
                            List<Map<String, Object>> byDept, List<Map<String, Object>> byEducation) {
}
