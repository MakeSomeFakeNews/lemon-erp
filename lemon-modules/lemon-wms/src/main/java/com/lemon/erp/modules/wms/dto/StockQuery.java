package com.lemon.erp.modules.wms.dto;

/**
 * 库存查询条件
 *
 * @param keyword  物料编码或名称，模糊匹配
 * @param batchNo  批次号，模糊匹配
 * @param showZero 是否显示数量为 0 的库存，默认不显示
 * @param lowStock  按物料汇总时，只看低于安全库存（库存下限）的物料
 * @param highStock 按物料汇总时，只看高于库存上限的物料；与 lowStock 同时勾选时两种都列出。
 *                  预警口径见 WmsMaterialWarehouseLimitMapper.xml 的 materialAlerts（按总库存的物料始终按所有仓库的合计判断）
 */
public record StockQuery(Long warehouseId, Long locationId, Long materialId, String keyword, String batchNo,
                         String category, Boolean showZero, Boolean lowStock, Boolean highStock) {

    public boolean hideZero() {
        return !Boolean.TRUE.equals(showZero);
    }
}
