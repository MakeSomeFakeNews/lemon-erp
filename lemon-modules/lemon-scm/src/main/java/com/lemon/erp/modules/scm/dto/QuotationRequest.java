package com.lemon.erp.modules.scm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 新增 / 修改销售报价单（草稿）
 *
 * @param wholeDiscount 整单优惠（含税，抹零也用它）：报价合计 = 明细合计 − 优惠，明细单价不变；转销售订单时按行金额分摊到订单各行
 * @param invoiceKind 发票类型（业务字典 scm_invoice_kind），选填：明细未填写税率时取它的税率，转销售订单时带到订单
 */
public record QuotationRequest(
        @NotNull(message = "{scm.valid.selectCustomer}") Long customerId,
        Long opportunityId,
        @NotNull(message = "{scm.valid.selectQuotationDate}") LocalDate quoteDate,
        LocalDate validTo,
        @Size(max = 255, message = "{scm.valid.remarkCannotExceedCharacters}") String remark,
        @NotEmpty(message = "{scm.valid.addLineItems}") @Size(max = 200, message = "{scm.valid.noMoreLines}") List<@Valid @NotNull Item> items,
        @DecimalMin(value = "0", message = "{scm.valid.wholeDiscountCannotNegative}")
        @Digits(integer = 16, fraction = 2, message = "{scm.valid.wholeDiscountDecimals}") BigDecimal wholeDiscount,
        @Size(max = 16) String invoiceKind) {

    /** 没有整单优惠 */
    public QuotationRequest(Long customerId, Long opportunityId, LocalDate quoteDate, LocalDate validTo, String remark, List<Item> items) {
        this(customerId, opportunityId, quoteDate, validTo, remark, items, null, null);
    }


    public record Item(
            @NotNull(message = "{scm.valid.selectMaterial}") Long materialId,
            @NotNull(message = "{scm.valid.enterQuantity}") @DecimalMin(value = "0", inclusive = false, message = "{scm.valid.quantityMustGreater}")
            @Digits(integer = 14, fraction = 4, message = "{scm.valid.quantityAllowsMostDecimalPlaces}") BigDecimal qty,
            @NotNull(message = "{scm.valid.enterUnitPrice}") @DecimalMin(value = "0", message = "{scm.valid.unitPriceCannotLess}")
            @Digits(integer = 14, fraction = 4, message = "{scm.valid.unitPriceAllowsMostDecimalPlaces}") BigDecimal price,
            @DecimalMin(value = "0", message = "{scm.valid.taxRateCannotNegative}") @DecimalMax(value = "100", message = "{scm.valid.taxRateCannotExceed}") BigDecimal taxRate,
            @Size(max = 255, message = "{scm.valid.remarkCannotExceedCharacters}") String remark) {
    }
}
