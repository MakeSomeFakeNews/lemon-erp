package com.lemon.erp.modules.wms.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * 入库类型：入库单的业务类型（字典 wms_in_type）。采购入库、生产入库、销售退货、生产退料、委外收回等由业务单据生成
 */
public enum StockInType implements StockDocType {
    /** 采购入库 */
    PURCHASE,
    /** 生产入库 */
    PRODUCTION,
    /** 销售退货 */
    SALES_RETURN,
    /** 其他入库 */
    OTHER,
    /** 生产退料 */
    PRODUCTION_RETURN,
    /** 委外收回 */
    OUTSOURCE_RETURN,
    /** 报废入库 */
    SCRAP_IN,
    /** 寄售入库：只能入供应商寄售仓 */
    CONSIGN_IN,
    /** 期初入库：系统启用时导入的期初库存，日期为启用期间的前一天 */
    OPENING,
    /** 组装拆卸：组装拆卸单生成的入库单 */
    ASSEMBLY;

    /** 手工入库允许的类型 */
    public static final Set<StockInType> MANUAL = EnumSet.of(OTHER, CONSIGN_IN);


    @Override
    public StockBizType kind() {
        return StockBizType.IN;
    }

    @Override
    public boolean isManual() {
        return MANUAL.contains(this);
    }

    @Override
    public String ioCategoryDict() {
        return this == OTHER ? WmsConstants.IN_CATEGORY_DICT : null;
    }
}
