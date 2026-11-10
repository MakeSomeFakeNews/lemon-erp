package com.lemon.erp.modules.mes.model;

/**
 * 工单的交期状态（工单完成情况报表），按计划完工日期判断
 */
public enum WoDueState {
    /** 按期完工：完工日期不晚于计划完工日期 */
    ON_TIME,
    /** 延期完工 */
    LATE,
    /** 逾期未完工：已下达 / 生产中，计划完工日期已过 */
    OVERDUE,
    /** 进行中：已下达 / 生产中，未到计划完工日期 */
    OPEN,
    /** 已关闭 */
    CLOSED
}
