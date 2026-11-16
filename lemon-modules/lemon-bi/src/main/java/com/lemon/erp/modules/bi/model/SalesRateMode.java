package com.lemon.erp.modules.bi.model;

/**
 * 滞销 / 畅销分析（issue #116）：销售比例 = 期间净销量 ÷ 销售基数
 */
public enum SalesRateMode {
    /** 滞销：销售比例不高于阈值（默认 80%） */
    SLOW,
    /** 畅销：销售比例不低于阈值（默认 10%） */
    HOT
}
