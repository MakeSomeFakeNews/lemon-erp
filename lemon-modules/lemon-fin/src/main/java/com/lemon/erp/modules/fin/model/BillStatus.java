package com.lemon.erp.modules.fin.model;

/**
 * 核销状态：应收应付单的核销状态（字典 fin_bill_status）
 */
public enum BillStatus {
    /** 未核销 */
    UNSETTLED,
    /** 部分核销 */
    PARTIAL,
    /** 已核销 */
    SETTLED
}
