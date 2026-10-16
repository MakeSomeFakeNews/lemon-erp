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
        @NotNull(message = "请选择成员") Long userId,
        @NotBlank(message = "请选择项目角色") String role,
        @NotNull(message = "请填写投入比例") @Min(value = 1, message = "投入比例为1%-100%") @Max(value = 100, message = "投入比例为1%-100%") Integer allocation,
        @DecimalMin(value = "0", message = "小时成本不能小于0") @Digits(integer = 8, fraction = 2, message = "金额最多2位小数") BigDecimal costRate,
        LocalDate joinDate,
        LocalDate leaveDate,
        @Size(max = 255, message = "备注不能超过255个字符") String remark) {
}
