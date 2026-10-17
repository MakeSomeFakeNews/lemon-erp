package com.lemon.erp.modules.scm.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 订单明细
 *
 * @param price        含税单价
 * @param taxRate      税率（%）
 * @param deliveryDate 交货日期，不填取订单的交货日期
 */
public record OrderItemRequest(
        @NotNull(message = "{scm.valid.selectMaterial}") Long materialId,
        @NotNull(message = "{scm.valid.enterQuantity}")
        @DecimalMin(value = "0", inclusive = false, message = "{scm.valid.quantityMustGreater}")
        @Digits(integer = 14, fraction = 4, message = "{scm.valid.quantityAllowsMostDecimalPlaces}") BigDecimal qty,
        @NotNull(message = "{scm.valid.enterUnitPrice}")
        @DecimalMin(value = "0", message = "{scm.valid.unitPriceCannotLess}")
        @Digits(integer = 14, fraction = 4, message = "{scm.valid.unitPriceAllowsMostDecimalPlaces}") BigDecimal price,
        @DecimalMin(value = "0", message = "{scm.valid.taxRateCannotNegative}") @DecimalMax(value = "100", message = "{scm.valid.taxRateCannotExceed}") BigDecimal taxRate,
        LocalDate deliveryDate,
        @Size(max = 255, message = "{scm.valid.remarkCannotExceedCharacters}") String remark,
        @Size(max = 16, message = "{scm.valid.unitCannotExceedCharacters}") String unitName) {

    /**
     * 数量、单价按基本单位；unitName 为录入时使用的辅助单位（仅用于显示，换算率取物料的辅助单位设置）
     */
    public OrderItemRequest(Long materialId, BigDecimal qty, BigDecimal price, BigDecimal taxRate, LocalDate deliveryDate, String remark) {
        this(materialId, qty, price, taxRate, deliveryDate, remark, null);
    }
}
