package com.lemon.erp.modules.wms.model;

import java.math.BigDecimal;

/**
 * 冻结 / 解冻的一行库存
 *
 * @param locationId 库位，0 为未指定
 * @param batchNo    批次号，未启用批次管理为空串
 * @param qty        数量（正数）
 */
public record HoldLine(Long warehouseId, long locationId, Long materialId, String batchNo, BigDecimal qty) {
}
