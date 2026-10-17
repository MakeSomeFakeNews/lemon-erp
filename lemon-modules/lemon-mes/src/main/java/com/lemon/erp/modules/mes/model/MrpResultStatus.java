package com.lemon.erp.modules.mes.model;

/**
 * MRP 运算结果的处理状态
 */
public enum MrpResultStatus {
    /** 待处理 */
    NEW,
    /** 已下达 */
    RELEASED,
    /** 无需处理 */
    NONE,
    /** 已失效（重新运算后旧结果作废） */
    EXPIRED
}
