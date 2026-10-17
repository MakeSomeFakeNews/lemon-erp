package com.lemon.erp.modules.mes.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 补料申请
 *
 * @param reason 字典 mes_supplement_reason
 */
public record SupplementRequest(
        @NotNull(message = "{mes.valid.selectWorkOrder}") Long woId,
        @NotBlank(message = "{mes.valid.selectSupplementaryIssueReason}") @Size(max = 16, message = "{mes.valid.invalidSupplementaryIssueReason}") String reason,
        @Size(max = 255, message = "{mes.valid.descriptionCannotExceedCharacters}") String remark,
        @NotEmpty(message = "{mes.valid.addSupplementaryIssueLines}") @Size(max = 200, message = "{mes.valid.noMoreLines}") List<@Valid @NotNull Item> items) {

    public record Item(
            @NotNull(message = "{mes.valid.selectMaterial}") Long materialId,
            @NotNull(message = "{mes.valid.enterQuantity}")
            @DecimalMin(value = "0", inclusive = false, message = "{mes.valid.quantityMustGreater}")
            @Digits(integer = 14, fraction = 4, message = "{mes.valid.quantityAllowsMostDecimalPlaces}") BigDecimal qty) {
    }
}
