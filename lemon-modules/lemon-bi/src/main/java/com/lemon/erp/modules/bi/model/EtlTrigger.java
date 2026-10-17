package com.lemon.erp.modules.bi.model;

/**
 * 数据仓库同步的触发方式
 */
public enum EtlTrigger {
    /** 手动 */
    MANUAL,
    /** 定时 */
    SCHEDULE
}
