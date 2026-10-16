package com.lemon.erp.modules.wms.dto;

/**
 * 库存查询条件
 *
 * @param keyword  物料编码或名称，模糊匹配
 * @param batchNo  批次号，模糊匹配
 * @param showZero 是否显示数量为 0 的库存，默认不显示
 * @param lowStock 按物料汇总时，只看低于安全库存的物料
 */
public record StockQuery(Long warehouseId, Long locationId, Long materialId, String keyword, String batchNo,
                         String category, Boolean showZero, Boolean lowStock) {

    public boolean hideZero() {
        return !Boolean.TRUE.equals(showZero);
    }
}
