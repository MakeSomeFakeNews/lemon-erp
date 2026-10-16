package com.lemon.erp.modules.crm.dto;

import com.lemon.erp.modules.wms.dto.StockDocItemRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * 服务工单领用备件：生成服务领用出库单（确认后扣库存，成本核算计入销售费用）
 */
public record TicketPartsRequest(
        @NotNull(message = "请选择仓库") Long warehouseId,
        @NotNull(message = "请选择领用日期") LocalDate bizDate,
        @NotEmpty(message = "请添加备件") @Size(max = 50, message = "备件不能超过50行") List<@Valid @NotNull StockDocItemRequest> items,
        @Size(max = 255, message = "备注不能超过255个字符") String remark) {
}
