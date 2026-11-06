package com.lemon.erp.modules.scm.model;

/**
 * 开单默认价的来源。销售：客户执行价 → 销售价目表 → 客户价格等级价 → 零售价 → 最近售价；
 * 采购：采购合同价 → 供货价格 → 最近进价 → 参考成本价
 */
public enum PriceSource {
    CUSTOMER_PRICE,
    PRICE_LIST,
    PRICE_LEVEL,
    RETAIL,
    CONTRACT_PRICE,
    SUPPLIER_PRICE,
    LAST_PRICE,
    REF_COST
}
