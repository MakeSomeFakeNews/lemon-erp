package com.lemon.erp.modules.scm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 新增 / 修改供货价格
 *
 * @param price     含税单价
 * @param taxRate   税率（%），默认 13
 * @param leadDays  采购提前期（天）
 * @param isDefault 设为该物料的默认供应商（同一物料只有一个默认供应商）
 * @param validFrom 有效期起（为空不限）
 * @param validTo   有效期止（为空不限），过期后采购订单提示价格已过期
 * @param tiers     阶梯价：采购数量达到起订数量时按阶梯单价
 */
public record SupplierPriceRequest(
        @NotNull(message = "{scm.valid.selectSupplier}") Long supplierId,
        @NotNull(message = "{scm.valid.selectMaterial}") Long materialId,
        @NotNull(message = "{scm.valid.enterUnitPrice}")
        @DecimalMin(value = "0", message = "{scm.valid.unitPriceCannotLess}")
        @Digits(integer = 14, fraction = 4, message = "{scm.valid.unitPriceAllowsMostDecimalPlaces}") BigDecimal price,
        @DecimalMin(value = "0", message = "{scm.valid.taxRateCannotNegative}") @DecimalMax(value = "100", message = "{scm.valid.taxRateCannotExceed}") BigDecimal taxRate,
        @Min(value = 0, message = "{scm.valid.leadTimeCannotLess2}") @Max(value = 365, message = "{scm.valid.leadTimeCannotExceedDays2}") Integer leadDays,
        @DecimalMin(value = "0", message = "{scm.valid.minimumOrderQuantityCannotLess}") BigDecimal minQty,
        Integer isDefault,
        @Size(max = 255, message = "{scm.valid.remarkCannotExceedCharacters}") String remark,
        java.time.LocalDate validFrom,
        java.time.LocalDate validTo,
        @Size(max = 10, message = "{scm.valid.priceTiersCannotExceed}") List<@Valid @NotNull Tier> tiers) {

    public record Tier(
            @NotNull(message = "{scm.valid.enterTierMinimumQuantity}") @DecimalMin(value = "0", inclusive = false, message = "{scm.valid.tierMinimumQuantityMustGreater}") BigDecimal minQty,
            @NotNull(message = "{scm.valid.enterTierUnitPrice}") @DecimalMin(value = "0", message = "{scm.valid.tierUnitPriceCannotLess}")
            @Digits(integer = 14, fraction = 4, message = "{scm.valid.unitPriceAllowsMostDecimalPlaces}") BigDecimal price) {
    }
}
