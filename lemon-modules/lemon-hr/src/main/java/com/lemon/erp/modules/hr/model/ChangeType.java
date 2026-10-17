package com.lemon.erp.modules.hr.model;

/**
 * 异动类型：人事异动的类型（字典 hr_change_type）
 */
public enum ChangeType {
    /** 入职 */
    ENTRY,
    /** 转正 */
    REGULAR,
    /** 调岗 */
    TRANSFER,
    /** 调薪 */
    SALARY,
    /** 离职 */
    LEAVE
}
