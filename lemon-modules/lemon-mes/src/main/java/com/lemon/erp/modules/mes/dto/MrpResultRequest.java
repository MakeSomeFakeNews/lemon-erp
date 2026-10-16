package com.lemon.erp.modules.mes.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 下达前调整建议：数量、采购件的供应商与单价
 */
public record MrpResultRequest(
        @NotNull(message = "请填写建议数量")
        @DecimalMin(value = "0", message = "数量不能小于0")
        @Digits(integer = 14, fraction = 4, message = "数量最多4位小数") BigDecimal suggestQty,
        Long supplierId,
        @DecimalMin(value = "0", message = "单价不能小于0") BigDecimal price) {
}
