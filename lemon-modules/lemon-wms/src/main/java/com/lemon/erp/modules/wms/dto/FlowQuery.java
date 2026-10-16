package com.lemon.erp.modules.wms.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 库存流水查询条件
 *
 * @param keyword 物料编码或名称，模糊匹配
 * @param batchNo 批次号，精确匹配（批次追踪）
 * @param docNo   单据号，精确匹配
 */
public record FlowQuery(String bizType, Long warehouseId, Long materialId, String keyword, String batchNo, String docNo,
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
}
