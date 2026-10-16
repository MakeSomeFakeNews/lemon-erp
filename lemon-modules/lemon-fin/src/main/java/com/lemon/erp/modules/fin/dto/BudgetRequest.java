package com.lemon.erp.modules.fin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 新增 / 修改预算
 *
 * @param deptId 部门，不填为全公司
 * @param months 1-12 月预算，12 个
 */
public record BudgetRequest(
        @NotNull(message = "请选择年度") @Min(2000) @Max(2999) Integer budgetYear,
        Long deptId,
        @NotNull(message = "请选择科目") Long accountId,
        @NotNull(message = "请填写各月预算") @Size(min = 12, max = 12, message = "请填写 12 个月的预算") List<BigDecimal> months,
        @Size(max = 255) String remark,
        @jakarta.validation.constraints.Pattern(regexp = "^(NONE|WARN|BLOCK)?$", message = "预算控制方式不正确") String controlMode) {
}
