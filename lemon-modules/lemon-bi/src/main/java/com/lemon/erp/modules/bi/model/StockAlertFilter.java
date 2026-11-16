package com.lemon.erp.modules.bi.model;

/**
 * 库存预警「仓库」标签页的预警筛选（issue #116），不传为全部；预警口径见 {@link com.lemon.erp.modules.wms.model.StockAlert}
 */
public enum StockAlertFilter {
    /** 低于库存下限 */
    LOW,
    /** 高于库存上限 */
    HIGH,
    /** 低于下限或高于上限 */
    ANY
}
