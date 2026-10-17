package com.lemon.erp.modules.crm.model;

/**
 * 回款状态：合同回款计划各期的回款状态（实际回款按计划日期先后分摊得出，不存库）
 */
public enum CollectStatus {
    /** 已回款 */
    RECEIVED,
    /** 部分回款 */
    PARTIAL,
    /** 逾期 */
    OVERDUE,
    /** 未到期 */
    PENDING
}
