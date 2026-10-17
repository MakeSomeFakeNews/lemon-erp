package com.lemon.erp.modules.crm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 销售合同（草稿）与回款计划
 */
public record ContractRequest(
        @NotBlank(message = "{crm.valid.enterContractName}") @Size(max = 128, message = "{crm.valid.contractNameCannotExceedCharacters}") String name,
        @NotNull(message = "{crm.valid.selectCustomer}") Long customerId,
        Long opportunityId,
        Long quotationId,
        @NotNull(message = "{crm.valid.selectSigningDate}") LocalDate signDate,
        LocalDate startDate,
        LocalDate endDate,
        @NotNull(message = "{crm.valid.enterContractAmount}") @DecimalMin(value = "0.01", message = "{crm.valid.contractAmountMustGreater}")
        @Digits(integer = 16, fraction = 2, message = "{crm.valid.amountAllowsMostDecimalPlaces}") BigDecimal amount,
        Long ownerId,
        @Size(max = 500, message = "{crm.valid.remarkCannotExceedCharacters}") String remark,
        @Size(max = 24, message = "{crm.valid.collectionPlanCannotExceedInstallments}") List<@Valid @NotNull Plan> plans) {

    public record Plan(@NotNull(message = "{crm.valid.selectPlannedCollectionDate}") LocalDate planDate,
                       @NotNull(message = "{crm.valid.enterPlannedCollectionAmount}") @DecimalMin(value = "0.01", message = "{crm.valid.collectionAmountMustGreater}")
                       @Digits(integer = 16, fraction = 2) BigDecimal amount,
                       @Size(max = 255) String remark) {
    }
}
