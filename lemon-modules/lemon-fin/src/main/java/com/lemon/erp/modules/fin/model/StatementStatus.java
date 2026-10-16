package com.lemon.erp.modules.fin.model;

/**
 * 供应商对账单状态，与字典 fin_statement_status 对应
 */
public enum StatementStatus {
    DRAFT,
    /** 已发送，待供应商确认 */
    SENT,
    CONFIRMED,
    /** 供应商有异议：可以删除后重新生成，或核对后再次发送 */
    DISPUTED
}
