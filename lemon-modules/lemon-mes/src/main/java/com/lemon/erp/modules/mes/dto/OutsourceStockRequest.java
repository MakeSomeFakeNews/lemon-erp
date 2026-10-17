package com.lemon.erp.modules.mes.dto;

import com.lemon.erp.modules.wms.dto.StockDocItemRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * 委外发料（委外发料出库单）/ 委外退料（委外收回入库单）
 */
public record OutsourceStockRequest(
        @NotNull(message = "{mes.valid.selectWarehouse}") Long warehouseId,
        @NotNull(message = "{mes.valid.selectDate}") LocalDate bizDate,
        @NotEmpty(message = "{mes.valid.addMaterials}") @Size(max = 100, message = "{mes.valid.detailsCannotExceedLines}") List<@Valid @NotNull StockDocItemRequest> items,
        @Size(max = 255, message = "{mes.valid.remarkCannotExceedCharacters}") String remark) {
}
