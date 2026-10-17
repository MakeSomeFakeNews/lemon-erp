package com.lemon.erp.modules.fin.model;

/**
 * 借款状态：员工借款的状态（字典 fin_loan_status）
 */
public enum LoanStatus {
    /** 草稿 */
    DRAFT,
    /** 审批中 */
    APPROVING,
    /** 待付款 */
    APPROVED,
    /** 未还清 */
    PAID,
    /** 已还清 */
    CLEARED
}
