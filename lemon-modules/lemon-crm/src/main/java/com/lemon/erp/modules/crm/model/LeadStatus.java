package com.lemon.erp.modules.crm.model;

/**
 * 线索状态：销售线索的状态（字典 crm_lead_status）
 */
public enum LeadStatus {
    /** 待跟进 */
    NEW,
    /** 跟进中 */
    FOLLOWING,
    /** 已转化 */
    CONVERTED,
    /** 无效 */
    INVALID;

    /** 未转化、未作废（待跟进、跟进中）：可以修改、转化、作废 */
    public boolean isOpen() {
        return this == NEW || this == FOLLOWING;
    }
}
