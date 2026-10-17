package com.lemon.erp.modules.wms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 上架推荐：入库仓库和各行的物料、批次
 */
public record PutawayRequest(
        @NotNull(message = "{wms.valid.selectWarehouse}") Long warehouseId,
        @NotEmpty(message = "{wms.valid.addLineItems}") @Size(max = 500, message = "{wms.valid.lineItemsCannotExceed2}") List<@Valid @NotNull Line> lines) {

    public record Line(@NotNull(message = "{wms.valid.selectMaterial}") Long materialId, @Size(max = 64) String batchNo) {
    }
}
