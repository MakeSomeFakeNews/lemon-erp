package com.lemon.erp.modules.mes.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * @param ratio    替代 1 个主料需要的替代料数量
 * @param priority 越小越优先，默认 1
 */
public record SubstituteRequest(
        @NotNull(message = "{mes.valid.selectSubstitute}") Long substituteId,
        @NotNull(message = "{mes.valid.enterSubstitutionRatio}")
        @DecimalMin(value = "0", inclusive = false, message = "{mes.valid.substitutionRatioMustGreater}")
        @Digits(integer = 12, fraction = 6, message = "{mes.valid.substitutionRatioAllowsMostDecimalPlaces}") BigDecimal ratio,
        Integer priority,
        @Size(max = 255, message = "{mes.valid.remarkCannotExceedCharacters}") String remark) {
}
