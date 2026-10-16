package com.lemon.erp.modules.wms.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 按物料汇总的库存
 */
@Data
public class StockSummaryView {
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String spec;
    private String unit;
    private String category;
    private BigDecimal safetyStock;
    private BigDecimal qty;

    /** 冻结数量（待检、不合格），不能出库 */
    private BigDecimal frozenQty;
}
