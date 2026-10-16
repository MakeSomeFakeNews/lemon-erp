package com.lemon.erp.modules.hr.dto;

import java.util.Collection;

/**
 * 员工查询
 *
 * @param keyword 工号、姓名或手机号
 * @param deptIds 部门及其下级部门（由 deptId 展开）
 */
public record EmployeeQuery(String keyword, Collection<Long> deptIds, String status, Long positionId, String empType) {
}
