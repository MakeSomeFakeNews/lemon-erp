package com.lemon.erp.modules.fin.model;

/**
 * 低值易耗品领用单状态，与字典 fin_consumable_status 对应
 */
public enum ConsumableStatus {
    DRAFT,
    /** 已生成部门领用出库单 */
    CONFIRMED
}
