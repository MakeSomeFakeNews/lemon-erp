package com.lemon.erp.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StatusRequest(
        @NotNull(message = "状态不能为空") @Min(value = 0, message = "状态只能是 0 或 1") @Max(value = 1, message = "状态只能是 0 或 1") Integer status) {
}
