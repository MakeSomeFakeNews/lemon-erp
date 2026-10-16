package com.lemon.erp.modules.wms.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 待写入的库存流水
 */
public record StockFlow(String bizType, Long docId, String docNo, Long warehouseId, long locationId, Long materialId,
                        String batchNo, BigDecimal qty, Long createBy, LocalDateTime createTime) {
}
