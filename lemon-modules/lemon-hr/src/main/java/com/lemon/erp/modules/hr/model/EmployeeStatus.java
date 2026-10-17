package com.lemon.erp.modules.hr.model;

/**
 * 员工状态：试用、正式、离职（字典 hr_emp_status）
 */
public enum EmployeeStatus {
    /** 试用期 */
    PROBATION,
    /** 正式 */
    REGULAR,
    /** 离职 */
    LEFT;

    /** 是否在职（试用或正式） */
    public boolean isOnJob() {
        return this != LEFT;
    }
}
