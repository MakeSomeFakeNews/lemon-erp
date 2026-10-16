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
        @NotNull(message = "请选择物料") Long materialId,
        @NotNull(message = "请填写数量")
        @DecimalMin(value = "0", inclusive = false, message = "数量必须大于0")
        @Digits(integer = 14, fraction = 4, message = "数量最多4位小数") BigDecimal qty,
        @NotNull(message = "请填写单价")
        @DecimalMin(value = "0", message = "单价不能小于0")
        @Digits(integer = 14, fraction = 4, message = "单价最多4位小数") BigDecimal price,
        @DecimalMin(value = "0", message = "税率不能小于0") @DecimalMax(value = "100", message = "税率不能大于100") BigDecimal taxRate,
        LocalDate deliveryDate,
        @Size(max = 255, message = "备注不能超过255个字符") String remark,
        @Size(max = 16, message = "单位不能超过16个字符") String unitName) {

    /**
     * 数量、单价按基本单位；unitName 为录入时使用的辅助单位（仅用于显示，换算率取物料的辅助单位设置）
     */
    public OrderItemRequest(Long materialId, BigDecimal qty, BigDecimal price, BigDecimal taxRate, LocalDate deliveryDate, String remark) {
        this(materialId, qty, price, taxRate, deliveryDate, remark, null);
    }
}
