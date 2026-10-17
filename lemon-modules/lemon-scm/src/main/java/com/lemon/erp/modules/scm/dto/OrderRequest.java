package com.lemon.erp.modules.scm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * 新增 / 修改采购、销售订单（草稿）
 *
 * @param partnerId   供应商 / 客户
 * @param warehouseId 默认收货 / 发货仓库，选填
 * @param contractId  引用的采购合同（只用于采购订单）：按合同价下单，校验合同期限和数量、金额上限
 * @param rebateAmount 使用返利抵扣的金额（只用于手工销售订单），不超过客户的返利余额
 */
public record OrderRequest(
        @NotNull(message = "{scm.valid.selectBusinessPartner}") Long partnerId,
        @NotNull(message = "{scm.valid.selectOrderDate}") LocalDate orderDate,
        LocalDate deliveryDate,
        Long warehouseId,
        @Size(max = 255, message = "{scm.valid.remarkCannotExceedCharacters}") String remark,
        @NotEmpty(message = "{scm.valid.addLineItems}") @Size(max = 500, message = "{scm.valid.lineItemsCannotExceed}") List<@Valid @NotNull OrderItemRequest> items,
        @Size(max = 8) String currency,
        @jakarta.validation.constraints.DecimalMin(value = "0", inclusive = false, message = "{scm.valid.exchangeRateMustGreater}") java.math.BigDecimal exchangeRate,
        Long contractId,
        @jakarta.validation.constraints.DecimalMin(value = "0", message = "{scm.valid.rebateDeductionCannotNegative}") java.math.BigDecimal rebateAmount) {

    /** 不使用返利抵扣 */
    public OrderRequest(Long partnerId, LocalDate orderDate, LocalDate deliveryDate, Long warehouseId, String remark,
                        List<OrderItemRequest> items, String currency, java.math.BigDecimal exchangeRate, Long contractId) {
        this(partnerId, orderDate, deliveryDate, warehouseId, remark, items, currency, exchangeRate, contractId, null);
    }

    /** 本位币订单 */
    public OrderRequest(Long partnerId, LocalDate orderDate, LocalDate deliveryDate, Long warehouseId, String remark,
                        List<OrderItemRequest> items) {
        this(partnerId, orderDate, deliveryDate, warehouseId, remark, items, null, null, null);
    }

    /** 不引用采购合同 */
    public OrderRequest(Long partnerId, LocalDate orderDate, LocalDate deliveryDate, Long warehouseId, String remark,
                        List<OrderItemRequest> items, String currency, java.math.BigDecimal exchangeRate) {
        this(partnerId, orderDate, deliveryDate, warehouseId, remark, items, currency, exchangeRate, null);
    }
}
