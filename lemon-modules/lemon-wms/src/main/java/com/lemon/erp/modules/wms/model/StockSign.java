package com.lemon.erp.modules.wms.model;

/**
 * 按库存数量筛选物料（盘点单「选择商品」的库存条件），不传为全部
 */
public enum StockSign {
    /** 库存 &gt; 0 */
    POSITIVE,
    /** 库存 = 0（含没有库存记录的） */
    ZERO,
    /** 库存 &lt; 0 */
    NEGATIVE
}
