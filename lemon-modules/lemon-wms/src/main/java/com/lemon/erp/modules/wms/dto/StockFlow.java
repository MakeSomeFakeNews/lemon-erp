package com.lemon.erp.modules.wms.dto;

import com.lemon.erp.modules.wms.model.StockBizType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 待写入的库存流水
 *
 * @param bizPrice   业务单价（来源单据的不含税单价），可为空
 * @param costPrice  成本单价，供应商寄售仓不计价为空
 * @param costAmount 成本金额，与数量同号
 */
public record StockFlow(StockBizType bizType, Long docId, String docNo, Long warehouseId, long locationId, Long materialId,
                        String batchNo, BigDecimal qty, BigDecimal bizPrice, BigDecimal costPrice, BigDecimal costAmount,
                        Long partnerId, String partnerName, Long createBy, LocalDateTime createTime) {
}
