package com.lemon.erp.modules.wms.dto;

import com.lemon.erp.modules.wms.model.StockAlert;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 分仓库预警：一个仓库的当前库存与上下限
 */
@Data
public class WarehouseAlertView {
    private Long warehouseId;
    private String warehouseName;
    private BigDecimal qty;
    private BigDecimal minQty;
    private BigDecimal maxQty;
    /** 低于下限 / 高于上限，正常为空 */
    private StockAlert alert;
}
