package com.lemon.erp.modules.fin.model;

/**
 * 明细账、日记账的行类型
 */
public enum LedgerRowType {
    /** 期初余额 */
    OPEN,
    /** 凭证分录 */
    ENTRY,
    /** 本日合计（日记账） */
    DAY,
    /** 本期合计 */
    TOTAL
}
