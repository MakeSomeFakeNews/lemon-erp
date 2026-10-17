package com.lemon.erp.modules.fin.model;

/**
 * 往来对账单的明细类型：应收 / 应付单增加余额，收付款、预收预付款减少余额
 */
public enum PartnerLedgerType {
    BILL,
    SETTLEMENT,
    ADVANCE
}
