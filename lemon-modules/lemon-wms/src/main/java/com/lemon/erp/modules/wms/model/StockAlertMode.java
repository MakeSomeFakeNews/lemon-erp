package com.lemon.erp.modules.wms.model;

/**
 * 物料的库存预警方式（字典 wms_stock_alert_mode）
 */
public enum StockAlertMode {
    /** 按总库存：安全库存（库存下限）、库存上限针对所有仓库的库存合计 */
    TOTAL,
    /**
     * 分仓库：每个仓库单独设置下限、上限（wms_material_warehouse_limit），按仓库判断，没有设置的仓库不预警；
     * 物料上的安全库存、库存上限为各仓库设置之和（自动计算，供补货建议、MRP 等按全公司计算的功能使用）
     */
    WAREHOUSE
}
