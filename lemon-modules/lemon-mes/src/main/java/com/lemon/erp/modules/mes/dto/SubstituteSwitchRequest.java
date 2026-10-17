package com.lemon.erp.modules.mes.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 工单用料切换替代料
 *
 * @param qty 被替代的主料数量（不超过主料待领数量），替代料需求 = qty × 替代比例
 */
public record SubstituteSwitchRequest(
        @NotNull(message = "{mes.valid.selectWorkOrder}") Long woId,
        @NotNull(message = "{mes.valid.selectPrimaryMaterial}") Long materialId,
        @NotNull(message = "{mes.valid.selectSubstitute}") Long substituteId,
        @NotNull(message = "{mes.valid.enterSubstitutionQuantity}")
        @DecimalMin(value = "0", inclusive = false, message = "{mes.valid.substitutionQuantityMustGreater}")
        @Digits(integer = 14, fraction = 4, message = "{mes.valid.quantityAllowsMostDecimalPlaces}") BigDecimal qty) {
}
