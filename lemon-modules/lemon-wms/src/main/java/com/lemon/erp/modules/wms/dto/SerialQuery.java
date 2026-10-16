package com.lemon.erp.modules.wms.dto;

/**
 * 序列号查询条件
 *
 * @param serialNo 序列号，模糊匹配
 * @param keyword  物料编码或名称，模糊匹配
 */
public record SerialQuery(String serialNo, Long materialId, String keyword, Long warehouseId, String batchNo,
                          String status) {
}
