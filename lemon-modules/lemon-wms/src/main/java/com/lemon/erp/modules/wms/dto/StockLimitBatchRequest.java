package com.lemon.erp.modules.wms.dto;

import com.lemon.erp.modules.wms.model.StockAlertMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 批量设置库存上下限（物料列表选中的物料）
 *
 * @param materialIds 物料，最多 500 个
 * @param alertMode   按总库存：设置物料的安全库存、库存上限，并清除分仓库的设置；
 *                    分仓库：设置所选仓库的下限、上限（两者都为空时删除该仓库的设置），物料改为分仓库预警，其他仓库的设置不变
 * @param warehouseId 分仓库时必填
 * @param minQty      安全库存（库存下限），为空表示不设置（清除）
 * @param maxQty      库存上限，为空表示不设置（清除）
 */
public record StockLimitBatchRequest(
        @NotEmpty(message = "{wms.valid.selectMaterials}") @Size(max = 500, message = "{wms.valid.tooManyMaterials}") List<Long> materialIds,
        @NotNull(message = "{wms.valid.alertModeRequired}") StockAlertMode alertMode,
        Long warehouseId,
        @DecimalMin(value = "0", message = "{wms.valid.safetyStockCannotLess}") BigDecimal minQty,
        @DecimalMin(value = "0", message = "{wms.valid.maxStockCannotLess}") BigDecimal maxQty) {
}
