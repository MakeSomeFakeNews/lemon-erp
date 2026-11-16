package com.lemon.erp.modules.bi.model;

/**
 * 库存预警的库存数量筛选（issue #116），不传为全部
 */
public enum StockQtyFilter {
    /** 库存 &gt; 0 */
    POSITIVE,
    /** 库存 = 0 */
    ZERO,
    /** 库存 &lt; 0 */
    NEGATIVE,
    /** 库存 ≠ 0 */
    NONZERO
}
