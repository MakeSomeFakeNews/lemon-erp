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
public record ReserveRequest(@NotEmpty(message = "请填写预留明细") List<@Valid @NotNull Line> lines) {

    public record Line(
            @NotNull(message = "请选择订单明细") Long orderItemId,
            @NotNull(message = "请选择仓库") Long warehouseId,
            @NotNull(message = "请填写预留数量") @DecimalMin(value = "0", inclusive = false, message = "预留数量必须大于0") BigDecimal qty) {
    }
}
