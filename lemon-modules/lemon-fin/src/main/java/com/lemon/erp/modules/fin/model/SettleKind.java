package com.lemon.erp.modules.fin.model;

/**
 * 收付款单种类（预收预付登记也使用）
 */
public enum SettleKind {
    /** 收款：往来单位为客户 */
    RECEIPT,
    /** 付款：往来单位为供应商 */
    PAYMENT;

    /** 收款核销应收单、付款核销应付单 */
    public BillKind billKind() {
        return this == RECEIPT ? BillKind.AR : BillKind.AP;
    }
}
