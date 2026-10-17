package com.lemon.erp.modules.scm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 录入一个供应商的报价：覆盖该供应商之前的报价；不报价的明细不传
 */
public record InquiryQuoteRequest(
        @NotNull(message = "{scm.valid.selectSupplier}") Long supplierId,
        @NotNull(message = "{scm.valid.enterQuotation}") @Size(max = 200, message = "{scm.valid.quotationLinesCannotExceed}") List<@Valid @NotNull Line> lines) {

    public record Line(
            @NotNull(message = "{scm.valid.missingRfqLine}") Long itemId,
            @NotNull(message = "{scm.valid.enterUnitPrice}") @DecimalMin(value = "0", message = "{scm.valid.unitPriceCannotLess}")
            @Digits(integer = 14, fraction = 4, message = "{scm.valid.unitPriceAllowsMostDecimalPlaces}") BigDecimal price,
            @DecimalMin(value = "0", message = "{scm.valid.taxRateCannotNegative}") @DecimalMax(value = "100", message = "{scm.valid.taxRateCannotExceed}") BigDecimal taxRate,
            @Min(value = 0, message = "{scm.valid.leadTimeCannotLess}") @Max(value = 365, message = "{scm.valid.leadTimeCannotExceedDays}") Integer leadDays,
            @Size(max = 255, message = "{scm.valid.remarkCannotExceedCharacters}") String remark) {
    }
}
