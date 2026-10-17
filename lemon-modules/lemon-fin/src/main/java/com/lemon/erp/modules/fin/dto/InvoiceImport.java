package com.lemon.erp.modules.fin.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 导入的发票：数电票 XML 解析结果，或税务数字账户导出的发票清单（Excel）的一行
 *
 * @param invoiceType 字典 fin_invoice_type，为空按数电票（专票）
 * @param amount      价税合计（红字发票为负数）
 * @param lines       明细行（XML 导入时有）
 */
public record InvoiceImport(
        @NotBlank(message = "{fin.valid.enterInvoiceNumber}") @Size(max = 32, message = "{fin.valid.invoiceNumberCannotExceedCharacters}") String invoiceNo,
        @Pattern(regexp = "^(SPECIAL|NORMAL|E_SPECIAL|E_NORMAL)?$", message = "{fin.valid.invalidInvoiceType}") String invoiceType,
        @NotNull(message = "{fin.valid.enterInvoiceDate}") LocalDate invoiceDate,
        @Size(max = 32, message = "{fin.valid.sellerTaxpayerIdCannotExceedCharacters}") String sellerTaxNo,
        @Size(max = 128, message = "{fin.valid.sellerNameCannotExceedCharacters}") String sellerName,
        @Size(max = 32, message = "{fin.valid.buyerTaxpayerIdCannotExceedCharacters}") String buyerTaxNo,
        @Size(max = 128, message = "{fin.valid.buyerNameCannotExceedCharacters}") String buyerName,
        @NotNull(message = "{fin.valid.enterTotalAmountIncludingTax}") @Digits(integer = 16, fraction = 2, message = "{fin.valid.amountAllowsMostDecimalPlaces}") BigDecimal amount,
        @NotNull(message = "{fin.valid.enterTaxAmount}") @Digits(integer = 16, fraction = 2, message = "{fin.valid.taxAmountAllowsMostDecimalPlaces}") BigDecimal taxAmount,
        List<Line> lines) {

    public record Line(String itemName, String spec, String unit, BigDecimal qty, BigDecimal price, BigDecimal amount,
                       BigDecimal taxRate, BigDecimal taxAmount) {
    }
}
