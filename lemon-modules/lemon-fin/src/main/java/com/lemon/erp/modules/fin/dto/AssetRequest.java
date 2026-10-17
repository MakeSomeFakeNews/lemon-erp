package com.lemon.erp.modules.fin.dto;

import com.lemon.erp.modules.fin.model.AssetStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 新增 / 修改固定资产卡片：已计提折旧的资产不能修改原值、残值率、年限、开始使用日期
 *
 * @param usefulMonths       使用年限（月）
 * @param expenseAccountId   折旧费用科目（末级）
 * @param openingMonths      期初已计提月数（系统上线前已使用的资产），仅新增时有效
 * @param openingAccumulated 期初累计折旧，仅新增时有效
 */
public record AssetRequest(
        @NotBlank(message = "{fin.valid.enterAssetCode}") @Pattern(regexp = "^[A-Za-z0-9_-]{1,32}$", message = "{fin.valid.assetCodeOnlyContainLettersDigits}") String code,
        @NotBlank(message = "{fin.valid.enterAssetName}") @Size(max = 64) String name,
        @NotBlank(message = "{fin.valid.selectCategory}") String category,
        @Size(max = 64) String model,
        Long deptId,
        Long custodianId,
        @Size(max = 128, message = "{fin.valid.locationCannotExceedCharacters}") String location,
        @NotNull(message = "{fin.valid.selectServiceDate}") LocalDate useDate,
        @NotNull(message = "{fin.valid.enterOriginalValue}") @DecimalMin(value = "0", inclusive = false, message = "{fin.valid.originalValueMustGreater}")
        @Digits(integer = 16, fraction = 2) BigDecimal originalValue,
        @DecimalMin(value = "0", message = "{fin.valid.residualValueRateCannotLess}") @DecimalMax(value = "100", message = "{fin.valid.residualValueRateCannotExceed}") BigDecimal residualRate,
        @NotNull(message = "{fin.valid.enterUsefulLife}") @Min(value = 1, message = "{fin.valid.usefulLifeMustLeastMonth}") @Max(value = 1200) Integer usefulMonths,
        @NotNull(message = "{fin.valid.selectDepreciationExpenseAccount}") Long expenseAccountId,
        AssetStatus status,
        @Size(max = 255) String remark,
        @Min(value = 0, message = "{fin.valid.openingDepreciatedMonthsCannotLess}") Integer openingMonths,
        @DecimalMin(value = "0", message = "{fin.valid.openingAccumulatedDepreciationCannotLess}") @Digits(integer = 16, fraction = 2) BigDecimal openingAccumulated,
        Long supplierId,
        @DecimalMin(value = "0", message = "{fin.valid.inputVatCannotLess}") @Digits(integer = 16, fraction = 2) BigDecimal taxAmount) {
}
