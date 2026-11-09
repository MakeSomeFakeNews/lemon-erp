package com.lemon.erp.modules.fin.model;

/**
 * 冲账单状态，与字典 fin_offset_status 对应
 */
public enum OffsetStatus {
    /** 待复核：可修改、删除 */
    DRAFT,
    /** 已复核：已生成红字应收 / 应付单，未核销时可取消复核 */
    REVIEWED
}
