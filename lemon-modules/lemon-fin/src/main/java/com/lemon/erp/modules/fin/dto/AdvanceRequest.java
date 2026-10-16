package com.lemon.erp.modules.fin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 预收（客户）/ 预付（供应商）登记
 */
public record AdvanceRequest(
        @NotBlank @Pattern(regexp = "^(RECEIPT|PAYMENT)$", message = "种类不正确") String kind,
        @NotNull(message = "请选择往来单位") Long partnerId,
        @NotNull(message = "请选择日期") LocalDate advanceDate,
        @NotNull(message = "请选择结算账户") Long accountId,
        @Size(max = 16) String method,
        @NotNull(message = "请填写金额") @DecimalMin(value = "0.01", message = "金额必须大于0")
        @Digits(integer = 16, fraction = 2, message = "金额最多2位小数") BigDecimal amount,
        @Size(max = 255, message = "备注不能超过255个字符") String remark) {
}
