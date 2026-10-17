package com.lemon.erp.modules.wms.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * 出库类型：出库单的业务类型（字典 wms_out_type）。销售出库、生产领料、采购退货、项目领料、委外发料、服务领用、部门领用等由业务单据生成
 */
public enum StockOutType implements StockDocType {
    /** 销售出库 */
    SALES,
    /** 生产领料 */
    PRODUCTION,
    /** 采购退货 */
    PURCHASE_RETURN,
    /** 其他出库 */
    OTHER,
    /** 项目领料 */
    PROJECT,
    /** 委外发料 */
    OUTSOURCE,
    /** 服务领用 */
    SERVICE,
    /** 报废出库 */
    SCRAP,
    /** 寄售消耗 */
    CONSIGN,
    /** 部门领用（低值易耗品） */
    CONSUME,
    /** 组装拆卸：组装拆卸单生成的出库单 */
    ASSEMBLY;

    /** 手工出库允许的类型 */
    public static final Set<StockOutType> MANUAL = EnumSet.of(OTHER, SCRAP);

    /** 不能发出过期批次的出库类型（报废、其他出库、采购退货可以） */
    public static final Set<StockOutType> NO_EXPIRED = EnumSet.of(SALES, PRODUCTION, PROJECT, OUTSOURCE, SERVICE);


    @Override
    public StockBizType kind() {
        return StockBizType.OUT;
    }

    @Override
    public boolean isManual() {
        return MANUAL.contains(this);
    }

    @Override
    public String ioCategoryDict() {
        return this == OTHER || this == SCRAP ? WmsConstants.OUT_CATEGORY_DICT : null;
    }

    /** 不能发出过期批次 */
    public boolean forbidsExpired() {
        return NO_EXPIRED.contains(this);
    }
}
