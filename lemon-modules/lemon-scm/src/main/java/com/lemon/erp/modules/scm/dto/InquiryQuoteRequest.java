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
 *
 * @param wholeDiscount 整单优惠承诺（含税，按本次报价各行 数量 × 单价 的合计），为空或 0 表示没有；定标生成采购订单时带到订单上
 */
public record InquiryQuoteRequest(
        @NotNull(message = "{scm.valid.selectSupplier}") Long supplierId,
        @NotNull(message = "{scm.valid.enterQuotation}") @Size(max = 200, message = "{scm.valid.quotationLinesCannotExceed}") List<@Valid @NotNull Line> lines,
        @DecimalMin(value = "0", message = "{scm.valid.wholeDiscountCannotNegative}")
        @Digits(integer = 16, fraction = 2, message = "{scm.valid.wholeDiscountDecimals}") BigDecimal wholeDiscount) {

    /** 没有整单优惠 */
    public InquiryQuoteRequest(Long supplierId, List<Line> lines) {
        this(supplierId, lines, null);
    }


    public record Line(
            @NotNull(message = "{scm.valid.missingRfqLine}") Long itemId,
            @NotNull(message = "{scm.valid.enterUnitPrice}") @DecimalMin(value = "0", message = "{scm.valid.unitPriceCannotLess}")
            @Digits(integer = 14, fraction = 4, message = "{scm.valid.unitPriceAllowsMostDecimalPlaces}") BigDecimal price,
            @DecimalMin(value = "0", message = "{scm.valid.taxRateCannotNegative}") @DecimalMax(value = "100", message = "{scm.valid.taxRateCannotExceed}") BigDecimal taxRate,
            @Min(value = 0, message = "{scm.valid.leadTimeCannotLess}") @Max(value = 365, message = "{scm.valid.leadTimeCannotExceedDays}") Integer leadDays,
            @Size(max = 255, message = "{scm.valid.remarkCannotExceedCharacters}") String remark) {
    }
}
