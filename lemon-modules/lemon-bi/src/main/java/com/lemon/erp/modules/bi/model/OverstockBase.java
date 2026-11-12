package com.lemon.erp.modules.bi.model;

/**
 * 积压分析的积压基数（issue #111）：积压数量 = 截至日库存 − 积压基数，大于 0 的列为积压
 */
public enum OverstockBase {
    /**
     * 库存上限：按总库存预警的物料取物料的库存上限；分仓库预警的取所选仓库的上限，不选仓库时为各仓库上限之和；
     * 没有设置上限的物料不参与
     */
    MAX_STOCK,
    /** 上个月销量：截至日所在月份的上一个自然月的净销量 */
    LAST_MONTH,
    /** 最近 N 天销量：截至日（含）往前 N 天的净销量 */
    RECENT_DAYS
}
