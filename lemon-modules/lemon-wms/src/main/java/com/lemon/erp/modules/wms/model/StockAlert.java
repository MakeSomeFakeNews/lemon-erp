package com.lemon.erp.modules.wms.model;

/**
 * 库存预警状态：按物料的预警方式判断（按总库存时比较所有仓库的合计；分仓库时任一仓库低于下限即为低于，
 * 否则任一仓库高于上限为高于）。正常时为空
 */
public enum StockAlert {
    /** 低于安全库存（库存下限） */
    LOW,
    /** 高于库存上限 */
    HIGH
}
