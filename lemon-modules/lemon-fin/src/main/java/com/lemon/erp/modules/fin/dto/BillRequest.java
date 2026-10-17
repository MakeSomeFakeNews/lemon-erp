package com.lemon.erp.modules.fin.dto;

import com.lemon.erp.modules.fin.model.BillKind;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 手工录入应收 / 应付单（期初余额等）
 *
 * @param kind    AR / AP
 * @param dueDate 到期日，不填按往来单位账期计算
 */
public record BillRequest(
        @NotNull(message = "{fin.valid.selectKind}") BillKind kind,
        @NotNull(message = "{fin.valid.selectBusinessPartner}") Long partnerId,
        @NotNull(message = "{fin.valid.selectDate}") LocalDate billDate,
        LocalDate dueDate,
        @NotNull(message = "{fin.valid.enterAmount}") @Digits(integer = 16, fraction = 2, message = "{fin.valid.amountAllowsMostDecimalPlaces}") BigDecimal amount,
        @Size(max = 255, message = "{fin.valid.remarkCannotExceedCharacters}") String remark) {
}
