package com.lemon.erp.modules.fin.model;

/**
 * 往来对账单的明细类型：应收 / 应付单增加余额，收付款、预收预付款、应收应付对冲减少余额
 */
public enum PartnerLedgerType {
    BILL,
    SETTLEMENT,
    ADVANCE,
    /** 应收应付对冲（issue #95） */
    NETTING
}
