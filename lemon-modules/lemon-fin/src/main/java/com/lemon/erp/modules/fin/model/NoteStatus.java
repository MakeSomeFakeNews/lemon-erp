package com.lemon.erp.modules.fin.model;

/**
 * 票据状态，与字典 fin_note_status 对应
 */
public enum NoteStatus {
    /** 应收票据：持有中 */
    HELD,
    /** 应收票据：背书的付款审批中 */
    ENDORSING,
    ENDORSED,
    DISCOUNTED,
    COLLECTED,
    DISHONORED,
    /** 应付票据：开票的付款审批中 */
    ISSUING,
    /** 应付票据：未兑付 */
    ISSUED,
    PAID,
    /** 应付票据：开票审批被拒绝 */
    CANCELLED
}
