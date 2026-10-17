package com.lemon.erp.modules.scm.model;

/**
 * 请购单状态：请购单的状态（字典 scm_requisition_status）
 */
public enum RequisitionStatus {
    /** 草稿 */
    DRAFT,
    /** 审批中 */
    APPROVING,
    /** 已批准 */
    APPROVED,
    /** 已下单 */
    ORDERED,
    /** 已关闭 */
    CLOSED;

    /** 可以关闭：草稿、已批准（未下单） */
    public boolean isClosable() {
        return this == DRAFT || this == APPROVED;
    }
}
