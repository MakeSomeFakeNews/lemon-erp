package com.lemon.erp.modules.pm.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 新增 / 修改项目
 *
 * @param managerId     项目经理，不填为当前用户；项目经理自动加入项目成员
 * @param salesOrderId  关联的销售订单（由订单创建项目），客户取订单客户
 * @param budgetControl 1 超预算控制：费用、领料超过成本预算（有分类预算时按分类）时不能录入
 */
public record ProjectRequest(
        @NotBlank(message = "{pm.valid.projectCodeRequired}")
        @Pattern(regexp = "^[A-Za-z0-9_-]{1,32}$", message = "{pm.valid.projectCodeMayOnlyContainLetters}") String code,
        @NotBlank(message = "{pm.valid.projectNameRequired}") @Size(max = 128, message = "{pm.valid.projectNameCannotExceedCharacters}") String name,
        @NotBlank(message = "{pm.valid.selectProjectType}") String type,
        Long customerId,
        Long opportunityId,
        Long salesOrderId,
        Long managerId,
        String priority,
        @NotNull(message = "{pm.valid.selectPlannedStartDate}") LocalDate planStart,
        @NotNull(message = "{pm.valid.selectPlannedEndDate}") LocalDate planEnd,
        @DecimalMin(value = "0", message = "{pm.valid.contractAmountCannotNegative}") @Digits(integer = 16, fraction = 2, message = "{pm.valid.amountAllowsMostDecimalPlaces}") BigDecimal contractAmount,
        @DecimalMin(value = "0", message = "{pm.valid.taxRateCannotNegative}") @DecimalMax(value = "100", message = "{pm.valid.taxRateCannotExceed}") BigDecimal taxRate,
        @Size(max = 1000, message = "{pm.valid.projectDescriptionCannotExceedCharacters}") String description,
        Integer budgetControl) {
}
