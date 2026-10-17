package com.lemon.erp.modules.mes.model;

/**
 * 齐套状态：工单用料的齐套情况（字典 mes_kit_status）
 */
public enum KitStatus {
    /** 齐套 */
    COMPLETE,
    /** 部分缺料 */
    PARTIAL,
    /** 缺料 */
    SHORT,
    /** 已领齐 */
    ISSUED
}
