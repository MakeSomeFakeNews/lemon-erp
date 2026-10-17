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
        @NotNull(message = "{crm.valid.selectWarehouse}") Long warehouseId,
        @NotNull(message = "{crm.valid.selectIssueDate}") LocalDate bizDate,
        @NotEmpty(message = "{crm.valid.addSpareParts}") @Size(max = 50, message = "{crm.valid.sparePartsCannotExceedRows}") List<@Valid @NotNull StockDocItemRequest> items,
        @Size(max = 255, message = "{crm.valid.remarkCannotExceedCharacters2}") String remark) {
}
