package com.lemon.erp.modules.fin.model;

/**
 * 其他收支单状态，与字典 fin_io_status 对应：待确认时可修改、删除；确认后生成凭证，进入出纳日记账和资金日报
 */
public enum IoStatus {
    DRAFT,
    CONFIRMED
}
