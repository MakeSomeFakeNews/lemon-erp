package com.lemon.erp.modules.fin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 供应商对账单的请求
 */
public final class StatementRequests {

    private StatementRequests() {
    }

    /** 生成对账单：期间内还没有对过账的应付单 */
    public record Generate(
            @NotNull(message = "{fin.valid.selectSupplier}") Long supplierId,
            @NotNull(message = "{fin.valid.selectStatementPeriod}") LocalDate beginDate,
            @NotNull(message = "{fin.valid.selectStatementPeriod}") LocalDate endDate,
            @Size(max = 255, message = "{fin.valid.remarkCannotExceedCharacters}") String remark) {
    }

    /** 供应商确认：确认时可填写发票信息，有异议时须说明 */
    public record Confirm(
            @NotNull(message = "{fin.valid.chooseConfirmDispute}") Boolean agree,
            @Size(max = 500, message = "{fin.valid.descriptionCannotExceedCharacters2}") String remark,
            @Size(max = 64) String invoiceNo,
            @DecimalMin(value = "0", message = "{fin.valid.invoiceAmountCannotLess}") @Digits(integer = 16, fraction = 2) BigDecimal invoiceAmount,
            LocalDate invoiceDate) {
    }

    /** 已确认的对账单补填发票信息 */
    public record Invoice(
            @NotNull(message = "{fin.valid.enterInvoiceNumber}") @Size(max = 64) String invoiceNo,
            @NotNull(message = "{fin.valid.enterInvoiceAmount}") @DecimalMin(value = "0", message = "{fin.valid.invoiceAmountCannotLess}") @Digits(integer = 16, fraction = 2) BigDecimal invoiceAmount,
            @NotNull(message = "{fin.valid.selectInvoiceDate}") LocalDate invoiceDate) {
    }
}
