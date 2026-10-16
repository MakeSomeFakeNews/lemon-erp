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
        @NotBlank(message = "请选择发票种类") @Pattern(regexp = "^(OUTPUT|INPUT)$", message = "发票种类不正确") String kind,
        @NotBlank(message = "请填写发票号码") @Size(max = 32, message = "发票号码不能超过32个字符") String invoiceNo,
        @Pattern(regexp = "^(SPECIAL|NORMAL)?$", message = "发票类型不正确") String invoiceType,
        @NotNull(message = "请选择往来单位") Long partnerId,
        @NotNull(message = "请选择开票日期") LocalDate invoiceDate,
        @NotNull(message = "请填写价税合计") @Digits(integer = 16, fraction = 2, message = "金额最多2位小数") BigDecimal amount,
        @NotNull(message = "请填写税额") @Digits(integer = 16, fraction = 2, message = "税额最多2位小数") BigDecimal taxAmount,
        @Size(max = 255, message = "备注不能超过255个字符") String remark,
        @NotEmpty(message = "请选择勾稽的单据") List<@Valid @NotNull Item> items) {

    public record Item(@NotNull(message = "请选择单据") Long billId,
                       @NotNull(message = "请填写勾稽金额") @Digits(integer = 16, fraction = 2, message = "金额最多2位小数") BigDecimal amount) {
    }
}
