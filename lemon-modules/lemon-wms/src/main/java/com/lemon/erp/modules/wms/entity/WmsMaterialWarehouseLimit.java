package com.lemon.erp.modules.wms.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 物料分仓库的库存上下限（物料的预警方式为分仓库时使用，见 {@link com.lemon.erp.modules.wms.model.StockAlertMode#WAREHOUSE}）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_material_warehouse_limit")
public class WmsMaterialWarehouseLimit extends BaseEntity {

    private Long materialId;

    private Long warehouseId;

    /** 库存下限（安全库存），该仓库库存低于时预警，为空不检查 */
    @DecimalMin(value = "0", message = "{wms.valid.safetyStockCannotLess}")
    private BigDecimal minQty;

    /** 库存上限，该仓库库存高于时预警，为空不检查 */
    @DecimalMin(value = "0", message = "{wms.valid.maxStockCannotLess}")
    private BigDecimal maxQty;
}
