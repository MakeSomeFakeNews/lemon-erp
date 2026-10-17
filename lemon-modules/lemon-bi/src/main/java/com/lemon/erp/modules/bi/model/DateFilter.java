package com.lemon.erp.modules.bi.model;

/**
 * 多维分析数据集的日期筛选方式
 */
public enum DateFilter {
    /** 按业务日期 */
    DATE,
    /** 按月份 */
    PERIOD,
    /** 按快照日期（不筛选时取最新快照） */
    SNAPSHOT
}
