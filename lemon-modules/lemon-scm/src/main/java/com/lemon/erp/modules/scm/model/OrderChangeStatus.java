package com.lemon.erp.modules.scm.model;

/**
 * 变更单状态：订单变更单的状态（字典 scm_change_status）
 */
public enum OrderChangeStatus {
    /** 审批中 */
    APPROVING,
    /** 已生效 */
    APPROVED,
    /** 已驳回 */
    REJECTED
}
