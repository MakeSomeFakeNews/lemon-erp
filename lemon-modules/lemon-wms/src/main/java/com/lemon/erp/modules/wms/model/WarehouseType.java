package com.lemon.erp.modules.wms.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * 仓库类型：不良品仓的库存不计入 MRP、齐套、可承诺量（字典 wms_warehouse_type）
 */
public enum WarehouseType {
    /** 普通仓 */
    NORMAL,
    /** 原料仓 */
    RAW,
    /** 成品仓 */
    FINISHED,
    /** 不良品仓 */
    DEFECT,
    /** 客户寄售仓 */
    CONSIGN_OUT,
    /** 供应商寄售仓 */
    CONSIGN_IN;

    /** 不计入可用库存的仓库类型：不良品仓、客户寄售仓、供应商寄售仓 */
    public static final Set<WarehouseType> UNAVAILABLE = EnumSet.of(DEFECT, CONSIGN_OUT, CONSIGN_IN);

    /** 寄售仓：必须关联往来单位（客户 / 供应商） */
    public boolean isConsign() {
        return this == CONSIGN_OUT || this == CONSIGN_IN;
    }
}
