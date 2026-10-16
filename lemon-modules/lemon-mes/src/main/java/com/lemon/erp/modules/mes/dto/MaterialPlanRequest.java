package com.lemon.erp.modules.mes.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 物料的生产计划属性 + 替代料（整体替换）；物料基础信息在仓库物料管理中维护
 *
 * @param supplyType  为空表示自动：有默认 BOM 为自制，否则为采购
 * @param fqcRequired 1 完工需成品检验
 */
public record MaterialPlanRequest(
        @Pattern(regexp = "^(MAKE|BUY|OUTSOURCE|PHANTOM)$", message = "物料属性不正确") String supplyType,
        @Min(value = 0, message = "提前期不能小于0") @Max(value = 365, message = "提前期不能超过365天") Integer leadDays,
        @DecimalMin(value = "0", inclusive = false, message = "最小批量必须大于0")
        @Digits(integer = 14, fraction = 4, message = "最小批量最多4位小数") BigDecimal minLot,
        @DecimalMin(value = "0", inclusive = false, message = "批量倍数必须大于0")
        @Digits(integer = 14, fraction = 4, message = "批量倍数最多4位小数") BigDecimal lotMultiple,
        @DecimalMin(value = "0", message = "允许超领比例不能小于0")
        @DecimalMax(value = "100", message = "允许超领比例不能超过100%") BigDecimal overIssueRate,
        @Size(max = 64, message = "计划员不能超过64个字符") String planner,
        Integer fqcRequired,
        @Size(max = 20, message = "替代料不能超过20个") List<@Valid @NotNull SubstituteRequest> substitutes) {
}
