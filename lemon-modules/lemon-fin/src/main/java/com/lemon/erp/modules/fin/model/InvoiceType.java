package com.lemon.erp.modules.fin.model;

/**
 * 发票类型（字典 fin_invoice_type）
 */
public enum InvoiceType {
    /** 增值税专用发票（纸质 / 税控电子） */
    SPECIAL,
    /** 普通发票 */
    NORMAL,
    /** 数电票（增值税专用发票） */
    E_SPECIAL,
    /** 数电票（普通发票） */
    E_NORMAL
}
