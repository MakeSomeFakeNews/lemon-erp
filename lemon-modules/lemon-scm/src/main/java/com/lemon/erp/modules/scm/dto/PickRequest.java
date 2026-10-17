package com.lemon.erp.modules.scm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 确认拣货：每个通知明细的拣货数量合计须等于通知数量
 */
public record PickRequest(@NotEmpty(message = "{scm.valid.enterPickingLines}") @Size(max = 1000, message = "{scm.valid.tooManyPickingLines}") List<@Valid @NotNull Line> picks) {

    public record Line(
            @NotNull(message = "{scm.valid.missingNoticeLine}") Long deliveryItemId,
            Long locationId,
            @Size(max = 64, message = "{scm.valid.batchNoCannotExceedCharacters}") String batchNo,
            @NotNull(message = "{scm.valid.enterPickedQuantity}") @DecimalMin(value = "0", inclusive = false, message = "{scm.valid.pickedQuantityMustGreater}") BigDecimal qty,
            String serialNos) {
    }
}
