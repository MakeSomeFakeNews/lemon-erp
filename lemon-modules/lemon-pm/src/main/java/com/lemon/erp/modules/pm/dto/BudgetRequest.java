package com.lemon.erp.modules.pm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * 编制项目成本预算：按成本类型，合计为项目成本预算
 */
public record BudgetRequest(@NotNull(message = "请填写预算") List<@Valid @NotNull Item> items) {

    public record Item(@NotBlank(message = "请选择成本类型") String costType,
                       @NotNull(message = "请填写预算金额") @DecimalMin(value = "0", message = "预算不能小于0")
                       @Digits(integer = 16, fraction = 2, message = "金额最多2位小数") BigDecimal amount) {
    }
}
