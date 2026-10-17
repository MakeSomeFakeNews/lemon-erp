package com.lemon.erp.modules.scm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 装一箱：重量、体积和箱内物料（发货通知明细 + 数量）
 */
public record PackageRequest(
        @DecimalMin(value = "0", message = "{scm.valid.weightCannotNegative}") @Digits(integer = 7, fraction = 3, message = "{scm.valid.invalidWeightFormat}") BigDecimal weight,
        @DecimalMin(value = "0", message = "{scm.valid.volumeCannotNegative}") @Digits(integer = 6, fraction = 4, message = "{scm.valid.invalidVolumeFormat}") BigDecimal volume,
        @Size(max = 255, message = "{scm.valid.remarkCannotExceedCharacters}") String remark,
        @NotEmpty(message = "{scm.valid.enterMaterialsBox}") @Size(max = 200) List<@Valid @NotNull Line> items) {

    public record Line(
            @NotNull(message = "{scm.valid.missingDeliveryNoticeLine}") Long deliveryItemId,
            @NotNull(message = "{scm.valid.enterQuantity}") @DecimalMin(value = "0", inclusive = false, message = "{scm.valid.quantityMustGreater}")
            @Digits(integer = 14, fraction = 4) BigDecimal qty) {
    }
}
