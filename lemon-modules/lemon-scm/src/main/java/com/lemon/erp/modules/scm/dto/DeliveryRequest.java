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
        @NotNull(message = "{scm.valid.selectSalesOrder}") Long orderId,
        @NotNull(message = "{scm.valid.selectShippingWarehouse}") Long warehouseId,
        LocalDate shipDate,
        @Size(max = 255, message = "{scm.valid.remarkCannotExceedCharacters}") String remark,
        @NotEmpty(message = "{scm.valid.addLineItems}") @Size(max = 500, message = "{scm.valid.lineItemsCannotExceed}") List<@Valid @NotNull Item> items) {

    public record Item(
            @NotNull(message = "{scm.valid.selectOrderLine}") Long orderItemId,
            @NotNull(message = "{scm.valid.enterQuantity}") @DecimalMin(value = "0", inclusive = false, message = "{scm.valid.quantityMustGreater}")
            @Digits(integer = 14, fraction = 4, message = "{scm.valid.quantityAllowsMostDecimalPlaces}") BigDecimal qty,
            @Size(max = 255, message = "{scm.valid.remarkCannotExceedCharacters}") String remark) {
    }
}
