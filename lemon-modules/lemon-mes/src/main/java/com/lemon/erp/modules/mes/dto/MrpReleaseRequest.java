package com.lemon.erp.modules.mes.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 下达 MRP 建议：自制件生成生产工单（草稿），采购件按供应商合并生成采购订单（草稿）
 *
 * @param warehouseId 工单的完工入库仓库、采购订单的收货仓库，选填
 */
public record MrpReleaseRequest(@NotEmpty(message = "{mes.valid.selectSuggestionsRelease}") List<Long> resultIds, Long warehouseId) {
}
