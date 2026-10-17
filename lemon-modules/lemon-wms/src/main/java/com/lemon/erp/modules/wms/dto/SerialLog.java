package com.lemon.erp.modules.wms.dto;

import com.lemon.erp.modules.wms.model.StockBizType;

import java.time.LocalDateTime;

/**
 * 待写入的序列号轨迹
 *
 * @param direction 1 入 -1 出
 */
public record SerialLog(Long materialId, String serialNo, StockBizType bizType, Long docId, String docNo, int direction,
                        Long warehouseId, long locationId, String batchNo, Long createBy, LocalDateTime createTime) {
}
