package com.lemon.erp.modules.fin.dto;

import com.lemon.erp.modules.fin.model.FeeBearer;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 账户调拨
 *
 * @param fee       手续费，计入财务费用，可以为 0
 * @param feeBearer 手续费支付账户，不填为转出账户支付
 * @param handlerId 经办人（用户），可空
 * @param ticketNo  票据号码（转账回单、支票号等），可空
 */
public record FundTransferRequest(
        @NotNull(message = "{fin.valid.selectTransferDate}") LocalDate transferDate,
        @NotNull(message = "{fin.valid.selectSourceAccount}") Long fromAccountId,
        @NotNull(message = "{fin.valid.selectDestinationAccount}") Long toAccountId,
        @NotNull(message = "{fin.valid.enterAmount}") @DecimalMin(value = "0", inclusive = false, message = "{fin.valid.amountMustGreater2}")
        @Digits(integer = 16, fraction = 2, message = "{fin.valid.amountAllowsMostDecimalPlaces}") BigDecimal amount,
        @DecimalMin(value = "0", message = "{fin.valid.feeCannotLess}") @Digits(integer = 16, fraction = 2, message = "{fin.valid.amountAllowsMostDecimalPlaces}") BigDecimal fee,
        FeeBearer feeBearer,
        Long handlerId,
        @Size(max = 64, message = "{fin.valid.ticketNoTooLong}") String ticketNo,
        @Size(max = 255, message = "{fin.valid.remarkCannotExceedCharacters}") String remark) {
}
