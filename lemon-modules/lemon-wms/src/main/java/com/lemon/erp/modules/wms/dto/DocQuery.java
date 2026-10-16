package com.lemon.erp.modules.wms.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 单据列表查询条件
 *
 * @param docNo       单据号，模糊匹配
 * @param bizType     入库 / 出库类型
 * @param warehouseId 仓库（调拨单匹配调出或调入仓库）
 */
public record DocQuery(String docNo, String bizType, Long warehouseId, String status,
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
}
