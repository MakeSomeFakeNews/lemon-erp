package com.lemon.erp.modules.fin.model;

/**
 * 发票种类：销项（开给客户）/ 进项（供应商开来）
 */
public enum InvoiceKind {
    /** 销项发票：勾稽应收单 */
    OUTPUT,
    /** 进项发票：勾稽应付单 */
    INPUT;

    /** 勾稽的应收应付单种类 */
    public BillKind billKind() {
        return this == OUTPUT ? BillKind.AR : BillKind.AP;
    }
}
