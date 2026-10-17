package com.lemon.erp.modules.wms.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 收发存汇总的查询条件
 *
 * @param warehouseId 仓库，为空时为全部仓库（不含调拨）
 * @param keyword     物料编码或名称
 */
public record InOutQuery(@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                         Long warehouseId, String category, String keyword) {
}
