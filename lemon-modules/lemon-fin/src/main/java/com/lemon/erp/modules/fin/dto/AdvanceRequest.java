package com.lemon.erp.modules.fin.dto;

import com.lemon.erp.modules.fin.model.SettleKind;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 预收（客户）/ 预付（供应商）登记
 */
public record AdvanceRequest(
        @NotNull(message = "{fin.valid.invalidKind}") SettleKind kind,
        @NotNull(message = "{fin.valid.selectBusinessPartner}") Long partnerId,
        @NotNull(message = "{fin.valid.selectDate}") LocalDate advanceDate,
        @NotNull(message = "{fin.valid.selectSettlementAccount}") Long accountId,
        @Size(max = 16) String method,
        @NotNull(message = "{fin.valid.enterAmount}") @DecimalMin(value = "0.01", message = "{fin.valid.amountMustGreater}")
        @Digits(integer = 16, fraction = 2, message = "{fin.valid.amountAllowsMostDecimalPlaces}") BigDecimal amount,
        @Size(max = 255, message = "{fin.valid.remarkCannotExceedCharacters}") String remark) {
}
