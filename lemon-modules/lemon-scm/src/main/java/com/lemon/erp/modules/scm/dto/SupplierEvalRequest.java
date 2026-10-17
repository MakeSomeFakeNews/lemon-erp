package com.lemon.erp.modules.scm.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 新增供应商评估，各项得分 0-100
 *
 * @param period 考核期间，如 2026-Q3、2026-09
 */
public record SupplierEvalRequest(
        @NotNull(message = "{scm.valid.selectSupplier}") Long supplierId,
        @NotBlank(message = "{scm.valid.enterEvaluationPeriod}") @Size(max = 16, message = "{scm.valid.evaluationPeriodCannotExceedCharacters}") String period,
        @NotNull(message = "{scm.valid.enterQualityScore}") @DecimalMin("0") @DecimalMax("100") BigDecimal qualityScore,
        @NotNull(message = "{scm.valid.enterDeliveryScore}") @DecimalMin("0") @DecimalMax("100") BigDecimal deliveryScore,
        @NotNull(message = "{scm.valid.enterPriceScore}") @DecimalMin("0") @DecimalMax("100") BigDecimal priceScore,
        @NotNull(message = "{scm.valid.enterServiceScore}") @DecimalMin("0") @DecimalMax("100") BigDecimal serviceScore,
        @Size(max = 255, message = "{scm.valid.commentCannotExceedCharacters}") String remark) {
}
