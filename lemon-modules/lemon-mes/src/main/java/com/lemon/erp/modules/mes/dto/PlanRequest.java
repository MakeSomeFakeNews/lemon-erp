package com.lemon.erp.modules.mes.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PlanRequest(
        @NotNull(message = "{mes.valid.selectProduct}") Long materialId,
        @NotNull(message = "{mes.valid.enterQuantity}")
        @DecimalMin(value = "0", inclusive = false, message = "{mes.valid.quantityMustGreater}")
        @Digits(integer = 14, fraction = 4, message = "{mes.valid.quantityAllowsMostDecimalPlaces}") BigDecimal qty,
        @NotNull(message = "{mes.valid.selectDueDate}") LocalDate dueDate,
        @Size(max = 255, message = "{mes.valid.remarkCannotExceedCharacters}") String remark) {
}
