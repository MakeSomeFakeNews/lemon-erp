package com.lemon.erp.modules.mes.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 新增 / 修改生产工单（草稿）
 *
 * @param bomId 不填使用产品的默认 BOM
 */
public record WorkOrderRequest(
        @NotNull(message = "{mes.valid.selectProduct}") Long materialId,
        Long bomId,
        @NotNull(message = "{mes.valid.enterQuantity}")
        @DecimalMin(value = "0", inclusive = false, message = "{mes.valid.quantityMustGreater}")
        @Digits(integer = 14, fraction = 4, message = "{mes.valid.quantityAllowsMostDecimalPlaces}") BigDecimal qty,
        LocalDate planStart,
        LocalDate planEnd,
        Long warehouseId,
        @Size(max = 255, message = "{mes.valid.remarkCannotExceedCharacters}") String remark) {
}
