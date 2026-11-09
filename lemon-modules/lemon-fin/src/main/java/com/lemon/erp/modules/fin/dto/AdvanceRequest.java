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
 *
 * @param amount       金额（外币为原币金额）
 * @param currency     币种，为空是本位币
 * @param exchangeRate 外币汇率，为空取收付日所在月份的记账汇率
 */
public record AdvanceRequest(
        @NotNull(message = "{fin.valid.invalidKind}") SettleKind kind,
        @NotNull(message = "{fin.valid.selectBusinessPartner}") Long partnerId,
        @NotNull(message = "{fin.valid.selectDate}") LocalDate advanceDate,
        @NotNull(message = "{fin.valid.selectSettlementAccount}") Long accountId,
        @Size(max = 16) String method,
        @NotNull(message = "{fin.valid.enterAmount}") @DecimalMin(value = "0.01", message = "{fin.valid.amountMustGreater}")
        @Digits(integer = 16, fraction = 2, message = "{fin.valid.amountAllowsMostDecimalPlaces}") BigDecimal amount,
        @Size(max = 255, message = "{fin.valid.remarkCannotExceedCharacters}") String remark,
        @Size(max = 8) String currency,
        @DecimalMin(value = "0", inclusive = false, message = "{fin.valid.exchangeRateMustGreater}") BigDecimal exchangeRate) {

    /** 本位币预收预付 */
    public AdvanceRequest(SettleKind kind, Long partnerId, LocalDate advanceDate, Long accountId, String method, BigDecimal amount, String remark) {
        this(kind, partnerId, advanceDate, accountId, method, amount, remark, null, null);
    }
}
