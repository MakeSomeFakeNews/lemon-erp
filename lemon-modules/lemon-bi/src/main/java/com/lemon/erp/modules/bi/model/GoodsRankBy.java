package com.lemon.erp.modules.bi.model;

/**
 * 畅销商品的排序依据
 */
public enum GoodsRankBy {
    /** 净销量 */
    QTY,
    /** 净销售额（不含税） */
    AMOUNT,
    /** 毛利 */
    PROFIT
}
