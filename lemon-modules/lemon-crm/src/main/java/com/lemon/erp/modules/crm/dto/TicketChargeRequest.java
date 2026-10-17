package com.lemon.erp.modules.crm.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 服务收费：为工单客户生成应收单（其他业务收入）
 *
 * @param amount  含税金额
 * @param taxRate 税率（%），默认 6
 */
public record TicketChargeRequest(
        @NotNull(message = "{crm.valid.enterChargeAmount}") @DecimalMin(value = "0.01", message = "{crm.valid.chargeAmountMustGreater}")
        @Digits(integer = 16, fraction = 2, message = "{crm.valid.amountAllowsMostDecimalPlaces}") BigDecimal amount,
        @DecimalMin(value = "0", message = "{crm.valid.taxRateCannotNegative}") @DecimalMax(value = "100", message = "{crm.valid.taxRateCannotExceed}") BigDecimal taxRate,
        @NotNull(message = "{crm.valid.selectChargeDate}") LocalDate billDate,
        @Size(max = 255, message = "{crm.valid.descriptionCannotExceedCharacters}") String remark) {
}
