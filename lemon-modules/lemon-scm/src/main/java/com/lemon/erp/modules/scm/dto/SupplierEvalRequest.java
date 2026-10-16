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
        @NotNull(message = "请选择供应商") Long supplierId,
        @NotBlank(message = "请填写考核期间") @Size(max = 16, message = "考核期间不能超过16个字符") String period,
        @NotNull(message = "请填写质量得分") @DecimalMin("0") @DecimalMax("100") BigDecimal qualityScore,
        @NotNull(message = "请填写交期得分") @DecimalMin("0") @DecimalMax("100") BigDecimal deliveryScore,
        @NotNull(message = "请填写价格得分") @DecimalMin("0") @DecimalMax("100") BigDecimal priceScore,
        @NotNull(message = "请填写服务得分") @DecimalMin("0") @DecimalMax("100") BigDecimal serviceScore,
        @Size(max = 255, message = "评语不能超过255个字符") String remark) {
}
