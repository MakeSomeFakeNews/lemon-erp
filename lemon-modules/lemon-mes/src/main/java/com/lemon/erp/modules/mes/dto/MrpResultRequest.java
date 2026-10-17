package com.lemon.erp.modules.mes.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 下达前调整建议：数量、采购件的供应商与单价
 */
public record MrpResultRequest(
        @NotNull(message = "{mes.valid.enterSuggestedQuantity}")
        @DecimalMin(value = "0", message = "{mes.valid.quantityCannotLess}")
        @Digits(integer = 14, fraction = 4, message = "{mes.valid.quantityAllowsMostDecimalPlaces}") BigDecimal suggestQty,
        Long supplierId,
        @DecimalMin(value = "0", message = "{mes.valid.unitPriceCannotLess}") BigDecimal price) {
}
