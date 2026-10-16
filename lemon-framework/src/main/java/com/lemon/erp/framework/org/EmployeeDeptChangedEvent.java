package com.lemon.erp.framework.org;

/**
 * 员工调岗（部门变化）：系统同步登录账号的部门，部门负责人审批、数据权限按新部门生效
 */
public record EmployeeDeptChangedEvent(Long employeeId, Long userId, Long deptId) {
}
