package com.lemon.erp.modules.wms.dto;

import com.lemon.erp.modules.wms.model.TransitState;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 单据列表查询条件
 *
 * @param docNo       单据号，模糊匹配
 * @param bizType     入库 / 出库类型
 * @param warehouseId 仓库（调拨单匹配调出或调入仓库）
 * @param ioCategory  收发类别（仅出入库单）
 * @param transitState 在途进度（仅调拨单）：未完结 / 已超过预计到达日期
 */
public record DocQuery(String docNo, String bizType, Long warehouseId, String status,
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                       String ioCategory,
                       TransitState transitState) {
}
