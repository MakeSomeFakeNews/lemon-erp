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
        @NotNull(message = "请选择仓库") Long warehouseId,
        @NotEmpty(message = "请添加明细") @Size(max = 500, message = "明细不能超过500行") List<@Valid @NotNull Line> lines) {

    public record Line(@NotNull(message = "请选择物料") Long materialId, @Size(max = 64) String batchNo) {
    }
}
