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
 * 新增 / 修改询价单（还没有报价时可以修改）
 */
public record InquiryRequest(
        @NotNull(message = "{scm.valid.selectRfqDate}") LocalDate inquiryDate,
        LocalDate deadline,
        @Size(max = 255, message = "{scm.valid.descriptionCannotExceedCharacters}") String remark,
        @NotEmpty(message = "{scm.valid.addLineItems}") @Size(max = 200, message = "{scm.valid.noMoreLines}") List<@Valid @NotNull Item> items) {

    public record Item(
            @NotNull(message = "{scm.valid.selectMaterial}") Long materialId,
            @NotNull(message = "{scm.valid.enterQuantity}") @DecimalMin(value = "0", inclusive = false, message = "{scm.valid.quantityMustGreater}")
            @Digits(integer = 14, fraction = 4, message = "{scm.valid.quantityAllowsMostDecimalPlaces}") BigDecimal qty,
            LocalDate requiredDate,
            @Size(max = 255, message = "{scm.valid.remarkCannotExceedCharacters}") String remark) {
    }
}
