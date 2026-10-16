package com.lemon.erp.modules.fin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 科目期初余额（整体替换），借方合计必须等于贷方合计
 */
public record AccountOpeningRequest(
        @NotNull(message = "请填写期初余额") @Size(max = 2000, message = "科目不能超过2000个") List<@Valid @NotNull Item> items) {

    public record Item(
            @NotNull(message = "请选择科目") Long accountId,
            @DecimalMin(value = "0", message = "借方金额不能小于0") @Digits(integer = 16, fraction = 2, message = "金额最多2位小数") BigDecimal debit,
            @DecimalMin(value = "0", message = "贷方金额不能小于0") @Digits(integer = 16, fraction = 2, message = "金额最多2位小数") BigDecimal credit) {
    }
}
