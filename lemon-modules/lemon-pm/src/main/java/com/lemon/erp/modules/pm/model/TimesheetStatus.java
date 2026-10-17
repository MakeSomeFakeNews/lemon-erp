package com.lemon.erp.modules.pm.model;

/**
 * 工时状态：工时的审批状态（字典 pm_timesheet_status）
 */
public enum TimesheetStatus {
    /** 待审批 */
    SUBMITTED,
    /** 已通过 */
    APPROVED,
    /** 已驳回 */
    REJECTED
}
