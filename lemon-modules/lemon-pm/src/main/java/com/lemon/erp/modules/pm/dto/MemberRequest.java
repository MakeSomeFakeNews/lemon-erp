package com.lemon.erp.modules.pm.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 项目成员
 *
 * @param costRate 小时成本，不填时按员工薪资档案计算（月度人工成本 / 174 小时）
 */
public record MemberRequest(
        @NotNull(message = "{pm.valid.selectMember}") Long userId,
        @NotBlank(message = "{pm.valid.selectProjectRole}") String role,
        @NotNull(message = "{pm.valid.enterAllocation}") @Min(value = 1, message = "{pm.valid.allocationMust}") @Max(value = 100, message = "{pm.valid.allocationMust}") Integer allocation,
        @DecimalMin(value = "0", message = "{pm.valid.hourlyCostCannotNegative}") @Digits(integer = 8, fraction = 2, message = "{pm.valid.amountAllowsMostDecimalPlaces}") BigDecimal costRate,
        LocalDate joinDate,
        LocalDate leaveDate,
        @Size(max = 255, message = "{pm.valid.remarkCannotExceedCharacters}") String remark) {
}
