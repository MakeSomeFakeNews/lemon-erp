package com.lemon.erp.modules.wms.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 库存明细（仓库 + 库位 + 物料 + 批次）
 */
@Data
public class StockView {
    private Long id;
    private Long warehouseId;
    private String warehouseName;
    private Long locationId;
    private String locationCode;
    private String locationName;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String spec;
    private String unit;
    private Integer batchEnabled;
    private Integer serialEnabled;
    private String batchNo;
    private LocalDate productionDate;
    private LocalDate expiryDate;
    private BigDecimal qty;

    /** 冻结数量（待检、不合格），不能出库 */
    private BigDecimal frozenQty;
}
