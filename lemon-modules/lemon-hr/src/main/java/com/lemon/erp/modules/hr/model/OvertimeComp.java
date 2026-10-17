package com.lemon.erp.modules.hr.model;

/**
 * 加班补偿方式：加班发加班费或转调休（字典 hr_overtime_comp），存在加班申请的 type 字段中
 */
public enum OvertimeComp {
    /** 加班费 */
    PAY,
    /** 转调休 */
    TO_LEAVE;

    /** 加班申请的补偿方式：转调休，其他（未填写）为加班费 */
    public static OvertimeComp of(String type) {
        return TO_LEAVE.name().equals(type) ? TO_LEAVE : PAY;
    }
}
