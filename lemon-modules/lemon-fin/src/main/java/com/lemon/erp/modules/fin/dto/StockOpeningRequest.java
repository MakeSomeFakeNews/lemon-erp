package com.lemon.erp.modules.fin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 存货期初（整体替换）：启用期间之前的期末结存数量和金额，作为启用期间成本核算的期初
 *
 * @param startPeriod 启用期间 yyyyMM，期初保存在它的上一期间
 */
public record StockOpeningRequest(
        @NotNull(message = "{fin.valid.selectStartPeriod}") Integer startPeriod,
        @NotNull(message = "{fin.valid.enterOpeningInventory}") @Size(max = 5000, message = "{fin.valid.materialsCannotExceed}") List<@Valid @NotNull Item> items) {

    public record Item(
            @NotNull(message = "{fin.valid.selectMaterial}") Long materialId,
            @NotNull(message = "{fin.valid.enterQuantity}") @DecimalMin(value = "0", message = "{fin.valid.quantityCannotLess}")
            @Digits(integer = 14, fraction = 4, message = "{fin.valid.quantityAllowsMostDecimalPlaces}") BigDecimal qty,
            @NotNull(message = "{fin.valid.enterAmount}") @DecimalMin(value = "0", message = "{fin.valid.amountCannotLess}")
            @Digits(integer = 16, fraction = 2, message = "{fin.valid.amountAllowsMostDecimalPlaces}") BigDecimal amount) {
    }
}
