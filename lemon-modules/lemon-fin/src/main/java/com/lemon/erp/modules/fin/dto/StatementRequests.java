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
            @NotNull(message = "请选择供应商") Long supplierId,
            @NotNull(message = "请选择对账期间") LocalDate beginDate,
            @NotNull(message = "请选择对账期间") LocalDate endDate,
            @Size(max = 255, message = "备注不能超过255个字符") String remark) {
    }

    /** 供应商确认：确认时可填写发票信息，有异议时须说明 */
    public record Confirm(
            @NotNull(message = "请选择确认或有异议") Boolean agree,
            @Size(max = 500, message = "说明不能超过500个字符") String remark,
            @Size(max = 64) String invoiceNo,
            @DecimalMin(value = "0", message = "发票金额不能小于0") @Digits(integer = 16, fraction = 2) BigDecimal invoiceAmount,
            LocalDate invoiceDate) {
    }

    /** 已确认的对账单补填发票信息 */
    public record Invoice(
            @NotNull(message = "请填写发票号码") @Size(max = 64) String invoiceNo,
            @NotNull(message = "请填写发票金额") @DecimalMin(value = "0", message = "发票金额不能小于0") @Digits(integer = 16, fraction = 2) BigDecimal invoiceAmount,
            @NotNull(message = "请选择开票日期") LocalDate invoiceDate) {
    }
}
