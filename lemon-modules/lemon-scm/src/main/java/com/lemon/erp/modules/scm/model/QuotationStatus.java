package com.lemon.erp.modules.scm.model;

/**
 * 报价单状态：销售报价单的状态（字典 scm_quotation_status）
 */
public enum QuotationStatus {
    /** 草稿 */
    DRAFT,
    /** 审批中 */
    APPROVING,
    /** 已批准 */
    APPROVED,
    /** 已成交 */
    ORDERED,
    /** 已失效 */
    LOST;

    /** 可以标记为丢单：草稿、已批准（未下单） */
    public boolean isLosable() {
        return this == DRAFT || this == APPROVED;
    }
}
