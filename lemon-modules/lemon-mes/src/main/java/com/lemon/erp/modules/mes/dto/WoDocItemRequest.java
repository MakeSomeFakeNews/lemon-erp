package com.lemon.erp.modules.mes.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @param materialId 领料 / 退料为工单用料中的物料，完工入库为工单产品
 */
public record WoDocItemRequest(
        @NotNull(message = "{mes.valid.selectMaterial}") Long materialId,
        Long locationId,
        @Size(max = 64, message = "{mes.valid.batchNoCannotExceedCharacters}") String batchNo,
        LocalDate productionDate,
        LocalDate expiryDate,
        @NotNull(message = "{mes.valid.enterQuantity}")
        @DecimalMin(value = "0", inclusive = false, message = "{mes.valid.quantityMustGreater}")
        @Digits(integer = 14, fraction = 4, message = "{mes.valid.quantityAllowsMostDecimalPlaces}") BigDecimal qty,
        String serialNos) {
}
