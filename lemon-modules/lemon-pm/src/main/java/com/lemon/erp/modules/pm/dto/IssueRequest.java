package com.lemon.erp.modules.pm.dto;

import com.lemon.erp.modules.wms.dto.StockDocItemRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * 项目领料：生成「项目领料」出库单并直接确认，按存货单位成本计入项目材料成本
 */
public record IssueRequest(
        @NotNull(message = "{pm.valid.selectWarehouse}") Long warehouseId,
        @NotNull(message = "{pm.valid.selectBusinessDate}") LocalDate bizDate,
        @Size(max = 255, message = "{pm.valid.remarkCannotExceedCharacters}") String remark,
        @NotEmpty(message = "{pm.valid.addIssueLines}") @Size(max = 200, message = "{pm.valid.noMoreLines}") List<@Valid @NotNull StockDocItemRequest> items) {
}
