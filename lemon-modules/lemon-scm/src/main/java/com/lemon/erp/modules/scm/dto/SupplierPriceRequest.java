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
        @NotNull(message = "请选择供应商") Long supplierId,
        @NotNull(message = "请选择物料") Long materialId,
        @NotNull(message = "请填写单价")
        @DecimalMin(value = "0", message = "单价不能小于0")
        @Digits(integer = 14, fraction = 4, message = "单价最多4位小数") BigDecimal price,
        @DecimalMin(value = "0", message = "税率不能小于0") @DecimalMax(value = "100", message = "税率不能大于100") BigDecimal taxRate,
        @Min(value = 0, message = "提前期不能小于0") @Max(value = 365, message = "提前期不能超过365天") Integer leadDays,
        @DecimalMin(value = "0", message = "最小起订量不能小于0") BigDecimal minQty,
        Integer isDefault,
        @Size(max = 255, message = "备注不能超过255个字符") String remark,
        java.time.LocalDate validFrom,
        java.time.LocalDate validTo,
        @Size(max = 10, message = "阶梯价不能超过10档") List<@Valid @NotNull Tier> tiers) {

    public record Tier(
            @NotNull(message = "请填写阶梯起订数量") @DecimalMin(value = "0", inclusive = false, message = "阶梯起订数量必须大于0") BigDecimal minQty,
            @NotNull(message = "请填写阶梯单价") @DecimalMin(value = "0", message = "阶梯单价不能小于0")
            @Digits(integer = 14, fraction = 4, message = "单价最多4位小数") BigDecimal price) {
    }
}
