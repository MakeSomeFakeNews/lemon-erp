package com.lemon.erp.modules.scm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 新建发货通知：销售订单的未出库明细
 */
public record DeliveryRequest(
        @NotNull(message = "请选择销售订单") Long orderId,
        @NotNull(message = "请选择发货仓库") Long warehouseId,
        LocalDate shipDate,
        @Size(max = 255, message = "备注不能超过255个字符") String remark,
        @NotEmpty(message = "请添加明细") @Size(max = 500, message = "明细不能超过500行") List<@Valid @NotNull Item> items) {

    public record Item(
            @NotNull(message = "请选择订单明细") Long orderItemId,
            @NotNull(message = "请填写数量") @DecimalMin(value = "0", inclusive = false, message = "数量必须大于0")
            @Digits(integer = 14, fraction = 4, message = "数量最多4位小数") BigDecimal qty,
            @Size(max = 255, message = "备注不能超过255个字符") String remark) {
    }
}
