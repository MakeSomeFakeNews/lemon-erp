package com.lemon.erp.modules.wms.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 一次库存变动，由 StockService 执行：更新库存、批次、序列号并记录流水
 *
 * @param locationId     库位，0 为未指定
 * @param batchNo        批次号，未启用批次管理为空串
 * @param qty            变动数量，正数增加、负数减少
 * @param serialNos      本次增加 / 减少的序列号，数量与 |qty| 一致；非序列号物料为空
 * @param productionDate 生产日期，仅增加时用于登记新批次
 * @param expiryDate     有效期至，仅增加时用于登记新批次
 */
public record StockChange(Long warehouseId, long locationId, Long materialId, String batchNo, BigDecimal qty,
                          List<String> serialNos, LocalDate productionDate, LocalDate expiryDate) {

    public static StockChange of(Long warehouseId, long locationId, Long materialId, String batchNo, BigDecimal qty,
                                 List<String> serialNos) {
        return new StockChange(warehouseId, locationId, materialId, batchNo, qty, serialNos, null, null);
    }

    public boolean increase() {
        return qty.signum() > 0;
    }
}
