package com.lemon.erp.modules.fin.model;

/**
 * 应收应付种类：应收单 / 应付单
 */
public enum BillKind {
    /** 应收：往来单位为客户 */
    AR,
    /** 应付：往来单位为供应商 */
    AP;

    /** 应收单对应收款、应付单对应付款 */
    public SettleKind settleKind() {
        return this == AR ? SettleKind.RECEIPT : SettleKind.PAYMENT;
    }

    /** 生成凭证的来源 */
    public VoucherSource voucherSource() {
        return this == AR ? VoucherSource.AR : VoucherSource.AP;
    }
}
