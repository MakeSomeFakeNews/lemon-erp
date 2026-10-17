package com.lemon.erp.modules.scm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 新增 / 修改请购单（草稿）
 */
public record RequisitionRequest(
        @NotNull(message = "{scm.valid.selectRequisitionDate}") LocalDate reqDate,
        @Size(max = 255, message = "{scm.valid.descriptionCannotExceedCharacters}") String remark,
        @NotEmpty(message = "{scm.valid.addLineItems}") @Size(max = 200, message = "{scm.valid.noMoreLines}") List<@Valid @NotNull Item> items) {

    /**
     * @param supplierId 建议供应商，选填
     */
    public record Item(
            @NotNull(message = "{scm.valid.selectMaterial}") Long materialId,
            @NotNull(message = "{scm.valid.enterQuantity}") @DecimalMin(value = "0", inclusive = false, message = "{scm.valid.quantityMustGreater}")
            @Digits(integer = 14, fraction = 4, message = "{scm.valid.quantityAllowsMostDecimalPlaces}") BigDecimal qty,
            LocalDate requiredDate,
            Long supplierId,
            @Size(max = 255, message = "{scm.valid.remarkCannotExceedCharacters}") String remark) {
    }
}
