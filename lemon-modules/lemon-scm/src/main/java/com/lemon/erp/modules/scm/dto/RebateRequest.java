package com.lemon.erp.modules.scm.dto;

import com.lemon.erp.modules.scm.model.RebateSettle;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 新增 / 修改返利协议（执行中）
 *
 * @param tiers 阶梯：销售额达到 minAmount 时返利率为 rate%，取达到的最高一档
 */
public record RebateRequest(
        @NotNull(message = "{scm.valid.selectCustomer}") Long customerId,
        @NotNull(message = "{scm.valid.selectStartDate}") LocalDate beginDate,
        @NotNull(message = "{scm.valid.selectEndDate}") LocalDate endDate,
        @NotNull(message = "{scm.valid.selectPayoutMethod}") RebateSettle settleMode,
        @NotEmpty(message = "{scm.valid.addRebateTiers}") @Size(max = 20, message = "{scm.valid.rebateTiersCannotExceed}") List<@Valid @NotNull Tier> tiers,
        @Size(max = 255, message = "{scm.valid.descriptionCannotExceedCharacters}") String remark) {

    public record Tier(
            @NotNull(message = "{scm.valid.enterSalesAmount}") @DecimalMin(value = "0", message = "{scm.valid.salesAmountCannotNegative}") @Digits(integer = 16, fraction = 2) BigDecimal minAmount,
            @NotNull(message = "{scm.valid.enterRebateRate}") @DecimalMin(value = "0", inclusive = false, message = "{scm.valid.rebateRateMustGreater}")
            @DecimalMax(value = "100", message = "{scm.valid.rebateRateCannotExceed}") BigDecimal rate) {
    }
}
