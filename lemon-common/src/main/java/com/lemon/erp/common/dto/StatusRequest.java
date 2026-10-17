package com.lemon.erp.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StatusRequest(
        @NotNull(message = "{common.valid.statusRequired}") @Min(value = 0, message = "{common.valid.statusMust}") @Max(value = 1, message = "{common.valid.statusMust}") Integer status) {
}
