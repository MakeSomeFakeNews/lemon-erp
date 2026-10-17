package com.lemon.erp.modules.fin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 发票登记：勾稽的应收（销项）/ 应付（进项）单金额合计须等于发票价税合计
 */
public record InvoiceRequest(
        @NotBlank(message = "{fin.valid.selectInvoiceKind}") @Pattern(regexp = "^(OUTPUT|INPUT)$", message = "{fin.valid.invalidInvoiceKind}") String kind,
        @NotBlank(message = "{fin.valid.enterInvoiceNumber}") @Size(max = 32, message = "{fin.valid.invoiceNumberCannotExceedCharacters}") String invoiceNo,
        @Pattern(regexp = "^(SPECIAL|NORMAL|E_SPECIAL|E_NORMAL)?$", message = "{fin.valid.invalidInvoiceType}") String invoiceType,
        @NotNull(message = "{fin.valid.selectBusinessPartner}") Long partnerId,
        @NotNull(message = "{fin.valid.selectInvoiceDate}") LocalDate invoiceDate,
        @NotNull(message = "{fin.valid.enterTotalAmountIncludingTax}") @Digits(integer = 16, fraction = 2, message = "{fin.valid.amountAllowsMostDecimalPlaces}") BigDecimal amount,
        @NotNull(message = "{fin.valid.enterTaxAmount}") @Digits(integer = 16, fraction = 2, message = "{fin.valid.taxAmountAllowsMostDecimalPlaces}") BigDecimal taxAmount,
        @Size(max = 255, message = "{fin.valid.remarkCannotExceedCharacters}") String remark,
        @NotEmpty(message = "{fin.valid.selectDocumentsMatch}") List<@Valid @NotNull Item> items) {

    public record Item(@NotNull(message = "{fin.valid.selectDocument}") Long billId,
                       @NotNull(message = "{fin.valid.enterMatchedAmount}") @Digits(integer = 16, fraction = 2, message = "{fin.valid.amountAllowsMostDecimalPlaces}") BigDecimal amount) {
    }
}
