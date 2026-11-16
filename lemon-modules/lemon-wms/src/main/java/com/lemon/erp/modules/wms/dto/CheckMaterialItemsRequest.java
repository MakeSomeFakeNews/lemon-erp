package com.lemon.erp.modules.wms.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 把选择的物料转为盘点明细（不保存）：每次最多 500 个物料，前端分批请求
 */
public record CheckMaterialItemsRequest(
        @NotNull(message = "{wms.valid.selectCountWarehouse}") Long warehouseId,
        @NotEmpty(message = "{wms.valid.selectCountMaterials}")
        @Size(max = 500, message = "{wms.valid.countMaterialsCannotExceed}") List<@NotNull Long> materialIds) {
}
