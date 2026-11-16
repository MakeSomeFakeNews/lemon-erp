package com.lemon.erp.modules.bi.model;

/**
 * 滞销 / 畅销分析的销售基数（issue #116），基数不大于 0 的商品不参与（销售比例无法计算）
 */
public enum SalesRateBase {
    /** 本期进货量：采购入库 − 采购退货 */
    PURCHASE,
    /** 期初库存 + 本期进货量 */
    OPENING_PURCHASE
}
