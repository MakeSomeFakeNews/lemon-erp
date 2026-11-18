package com.lemon.erp.modules.fin.model;

/**
 * 对账单（供应商对账单、客户对账单）状态，与字典 fin_statement_status 对应
 */
public enum StatementStatus {
    DRAFT,
    /** 已发送，待对方确认 */
    SENT,
    CONFIRMED,
    /** 对方有异议：可以删除后重新生成，或核对后再次发送 */
    DISPUTED
}
