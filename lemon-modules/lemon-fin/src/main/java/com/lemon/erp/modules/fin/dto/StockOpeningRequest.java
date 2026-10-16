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
        @NotNull(message = "请选择启用期间") Integer startPeriod,
        @NotNull(message = "请填写存货期初") @Size(max = 5000, message = "物料不能超过5000个") List<@Valid @NotNull Item> items) {

    public record Item(
            @NotNull(message = "请选择物料") Long materialId,
            @NotNull(message = "请填写数量") @DecimalMin(value = "0", message = "数量不能小于0")
            @Digits(integer = 14, fraction = 4, message = "数量最多4位小数") BigDecimal qty,
            @NotNull(message = "请填写金额") @DecimalMin(value = "0", message = "金额不能小于0")
            @Digits(integer = 16, fraction = 2, message = "金额最多2位小数") BigDecimal amount) {
    }
}
