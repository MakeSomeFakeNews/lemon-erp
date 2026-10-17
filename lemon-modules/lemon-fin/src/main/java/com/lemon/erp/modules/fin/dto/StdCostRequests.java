package com.lemon.erp.modules.fin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 标准成本的请求
 */
public final class StdCostRequests {

    private StdCostRequests() {
    }

    /**
     * 新建版本：可复制已有版本的采购件单价，然后按当前默认 BOM 卷算
     *
     * @param effectivePeriod 生效期间 yyyyMM
     * @param copyFromId      复制的版本，为空时采购件单价取最近一次成本核算的单价（没有时取参考成本价）
     */
    public record Version(
            @NotBlank(message = "{fin.valid.enterVersionName}") @Size(max = 64, message = "{fin.valid.versionNameCannotExceedCharacters}") String name,
            @NotNull(message = "{fin.valid.selectEffectivePeriod}") @Min(value = 190001, message = "{fin.valid.invalidEffectivePeriodFormat}")
            @Max(value = 299912, message = "{fin.valid.invalidEffectivePeriodFormat}") Integer effectivePeriod,
            Long copyFromId,
            @Size(max = 255, message = "{fin.valid.descriptionCannotExceedCharacters}") String remark) {
    }

    /** 采购件标准单价（新增或修改） */
    public record Price(
            @NotNull(message = "{fin.valid.selectMaterial}") Long materialId,
            @NotNull(message = "{fin.valid.enterStandardUnitPrice}") @DecimalMin(value = "0", message = "{fin.valid.standardUnitPriceCannotNegative}")
            @Digits(integer = 12, fraction = 6, message = "{fin.valid.invalidStandardUnitPriceFormat}") BigDecimal price) {
    }
}
