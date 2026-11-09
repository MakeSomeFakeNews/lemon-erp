package com.lemon.erp.modules.wms.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 经营历史查询条件
 *
 * @param materialId  物料，必填
 * @param warehouseId 仓库，不选为全公司（不含调拨和供应商寄售仓）
 * @param beginDate   开始日期，默认本月 1 日
 * @param endDate     结束日期，默认今天
 */
public record StockHistoryQuery(Long materialId, Long warehouseId,
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
}
