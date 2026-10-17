package com.lemon.erp.modules.crm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 新增 / 修改商机。有产品明细时金额为明细合计；赢单后生成销售订单需要产品明细。
 * 销售阶段通过「推进阶段」修改，新增时默认初步接洽
 *
 * @param probability 赢单概率，不填按阶段默认
 */
public record OpportunityRequest(
        @NotBlank(message = "{crm.valid.opportunityNameRequired}") @Size(max = 128, message = "{crm.valid.opportunityNameCannotExceedCharacters}") String name,
        @NotNull(message = "{crm.valid.selectCustomer}") Long customerId,
        Long contactId,
        @DecimalMin(value = "0", message = "{crm.valid.amountCannotLess}") @Digits(integer = 16, fraction = 2, message = "{crm.valid.amountAllowsMostDecimalPlaces}") BigDecimal amount,
        String stage,
        @Min(value = 0, message = "{crm.valid.winProbabilityMustBetween}") @Max(value = 100, message = "{crm.valid.winProbabilityMustBetween}") Integer probability,
        LocalDate expectedDate,
        String source,
        Long campaignId,
        Long ownerId,
        @Size(max = 500, message = "{crm.valid.remarkCannotExceedCharacters}") String remark,
        @Size(max = 100, message = "{crm.valid.productLinesCannotExceedRows}") List<@Valid @NotNull Item> items) {

    public record Item(
            @NotNull(message = "{crm.valid.selectProduct}") Long materialId,
            @NotNull(message = "{crm.valid.enterQuantity}") @DecimalMin(value = "0", inclusive = false, message = "{crm.valid.quantityMustGreater}")
            @Digits(integer = 14, fraction = 4, message = "{crm.valid.quantityAllowsMostDecimalPlaces}") BigDecimal qty,
            @NotNull(message = "{crm.valid.enterUnitPrice}") @DecimalMin(value = "0", message = "{crm.valid.unitPriceCannotLess}")
            @Digits(integer = 14, fraction = 4, message = "{crm.valid.unitPriceAllowsMostDecimalPlaces}") BigDecimal price,
            @DecimalMin(value = "0", message = "{crm.valid.taxRateCannotNegative}") @DecimalMax(value = "100", message = "{crm.valid.taxRateCannotExceed}") BigDecimal taxRate) {
    }
}
