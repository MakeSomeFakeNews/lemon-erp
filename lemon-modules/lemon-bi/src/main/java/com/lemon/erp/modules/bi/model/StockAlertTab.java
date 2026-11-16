package com.lemon.erp.modules.bi.model;

/**
 * 库存预警的标签页（issue #116）
 */
public enum StockAlertTab {
    /** 仓库：每个仓库的商品库存明细与上下限 */
    WAREHOUSE,
    /** 高于上限：库存数量 &gt; 库存上限，积压数量 = 库存 − 上限 */
    HIGH,
    /** 低于下限：库存数量 &lt; 库存下限，补货数量 = 下限 − 库存 */
    LOW
}
