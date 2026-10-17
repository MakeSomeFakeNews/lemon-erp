package com.lemon.erp.modules.fin.model;

/**
 * 发票来源：登记方式
 */
public enum InvoiceSource {
    /** 手工登记 */
    MANUAL,
    /** 导入电子发票 XML */
    XML,
    /** Excel 批量导入 */
    EXCEL
}
