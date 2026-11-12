package com.lemon.erp.modules.wms.model;

/**
 * 调拨价格（调拨单表头的「调拨价格」）：同价调拨，或差价调拨的快速计算依据（调拨价 = 依据价 × 系数）。
 * 系统的库存成本按物料全公司维护（不分仓库），调拨不改变存货成本：调拨价是单据上的内部结算价，用于分店 / 仓库考核
 */
public enum TransferPricing {
    /** 同价调拨：调拨价 = 调出成本价（确认时生成），不能修改 */
    SAME,
    /** 参考成本价：差价调拨，依据物料的参考成本价 */
    REF_COST,
    /** 库存均价：差价调拨，依据当前移动加权平均成本 */
    AVG_COST,
    /** 最低售价：差价调拨，依据价格控制中的最低销售价 */
    MIN_SALE,
    /** 价格列：差价调拨，依据零售价 / 批发价 1-8（见 priceLevel） */
    PRICE_LEVEL;

    /** 差价调拨：调拨价可以与调出成本价不同 */
    public boolean differential() {
        return this != SAME;
    }
}
