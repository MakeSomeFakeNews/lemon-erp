package com.lemon.erp.modules.hr.dto;

/**
 * 选择员工的下拉选项
 */
public record EmployeeOption(Long id, String empNo, String name, Long deptId, String deptName, Long userId) {
}
