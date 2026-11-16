package com.lemon.erp.modules.bi.model;

/**
 * 周转分析的数据筛选（issue #116），不传为全部
 */
public enum TurnoverOutFilter {
    /** 不显示本期出库为 0 的商品 */
    HAS_OUT,
    /** 只显示本期出库为 0 的商品 */
    NO_OUT
}
