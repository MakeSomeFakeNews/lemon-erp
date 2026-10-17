package com.lemon.erp.modules.mes.model;

/**
 * 生产看板中工单的交期风险
 */
public enum DeliveryRisk {
    /** 预计完工不晚于交期 */
    ON_TIME,
    /** 预计完工晚于交期 */
    AT_RISK,
    /** 交期已过还没有完工 */
    OVERDUE,
    /** 产能不足，无法估算完工日期 */
    UNKNOWN
}
