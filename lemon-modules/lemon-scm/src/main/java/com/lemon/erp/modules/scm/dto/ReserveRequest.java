package com.lemon.erp.modules.scm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * 销售订单预留库存
 */
public record ReserveRequest(@NotEmpty(message = "{scm.valid.enterReservationLines}") List<@Valid @NotNull Line> lines) {

    public record Line(
            @NotNull(message = "{scm.valid.selectOrderLine}") Long orderItemId,
            @NotNull(message = "{scm.valid.selectWarehouse}") Long warehouseId,
            @NotNull(message = "{scm.valid.enterReservedQuantity}") @DecimalMin(value = "0", inclusive = false, message = "{scm.valid.reservedQuantityMustGreater}") BigDecimal qty) {
    }
}
