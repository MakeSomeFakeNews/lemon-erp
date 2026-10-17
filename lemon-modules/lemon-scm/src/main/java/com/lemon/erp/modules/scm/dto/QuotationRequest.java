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
 */
public record QuotationRequest(
        @NotNull(message = "{scm.valid.selectCustomer}") Long customerId,
        Long opportunityId,
        @NotNull(message = "{scm.valid.selectQuotationDate}") LocalDate quoteDate,
        LocalDate validTo,
        @Size(max = 255, message = "{scm.valid.remarkCannotExceedCharacters}") String remark,
        @NotEmpty(message = "{scm.valid.addLineItems}") @Size(max = 200, message = "{scm.valid.noMoreLines}") List<@Valid @NotNull Item> items) {

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
