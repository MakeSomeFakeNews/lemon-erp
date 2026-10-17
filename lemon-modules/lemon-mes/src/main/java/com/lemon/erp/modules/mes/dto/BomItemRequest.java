package com.lemon.erp.modules.mes.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * @param qty      单位用量：生产 1 个产品需要的数量
 * @param lossRate 损耗率（%）
 * @param refDes   位号，如 R1,R2 或区间 C1-C8，见 {@link com.lemon.erp.modules.mes.model.RefDes}
 */
public record BomItemRequest(
        @NotNull(message = "{mes.valid.selectComponent}") Long materialId,
        @NotNull(message = "{mes.valid.enterQuantity2}")
        @DecimalMin(value = "0", inclusive = false, message = "{mes.valid.quantityMustGreater2}")
        @Digits(integer = 12, fraction = 6, message = "{mes.valid.quantityAllowsMostDecimalPlaces2}") BigDecimal qty,
        @DecimalMin(value = "0", message = "{mes.valid.lossRateCannotLess}") @DecimalMax(value = "100", message = "{mes.valid.lossRateCannotGreater}") BigDecimal lossRate,
        @Size(max = 1000, message = "{mes.valid.referenceDesignatorsCannotExceedCharacters}") String refDes,
        @Size(max = 255, message = "{mes.valid.remarkCannotExceedCharacters}") String remark) {
}
