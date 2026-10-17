package com.lemon.erp.modules.fin.model;

/**
 * 报销状态：费用报销单的状态（字典 fin_claim_status）
 */
public enum ExpenseClaimStatus {
    /** 草稿 */
    DRAFT,
    /** 审批中 */
    APPROVING,
    /** 待付款 */
    APPROVED,
    /** 已付款 */
    PAID
}
