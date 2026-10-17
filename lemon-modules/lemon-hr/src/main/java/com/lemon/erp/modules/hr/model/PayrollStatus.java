package com.lemon.erp.modules.hr.model;

/**
 * 工资表状态：工资表的状态（字典 hr_payroll_status）。草稿 →（审批中）→ 已确认 → 已发放
 */
public enum PayrollStatus {
    /** 草稿 */
    DRAFT,
    /** 已确认 */
    CONFIRMED,
    /** 已发放 */
    PAID,
    /** 审批中 */
    APPROVING
}
