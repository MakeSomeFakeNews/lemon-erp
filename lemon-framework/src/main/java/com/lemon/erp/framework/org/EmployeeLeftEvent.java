package com.lemon.erp.framework.org;

import java.time.LocalDate;

/**
 * 员工离职（人事异动生效时在同一事务中同步发布）：系统停用登录账号，客户关系、项目、审批待办按交接人移交
 *
 * @param userId         离职员工关联的登录账号，没有关联时为空
 * @param handoverUserId 工作交接人，为空时客户退回公海、待办保留给管理员处理
 */
public record EmployeeLeftEvent(Long employeeId, Long userId, Long handoverUserId, LocalDate leaveDate) {
}
