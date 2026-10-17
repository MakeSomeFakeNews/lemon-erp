package com.lemon.erp.modules.mes.dto;

import com.lemon.erp.modules.mes.model.SupplyType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
        SupplyType supplyType,
        @Min(value = 0, message = "{mes.valid.leadTimeCannotLess}") @Max(value = 365, message = "{mes.valid.leadTimeCannotExceedDays}") Integer leadDays,
        @DecimalMin(value = "0", inclusive = false, message = "{mes.valid.minimumLotSizeMustGreater}")
        @Digits(integer = 14, fraction = 4, message = "{mes.valid.minimumLotSizeAllowsMostDecimal}") BigDecimal minLot,
        @DecimalMin(value = "0", inclusive = false, message = "{mes.valid.lotMultipleMustGreater}")
        @Digits(integer = 14, fraction = 4, message = "{mes.valid.lotMultipleAllowsMostDecimalPlaces}") BigDecimal lotMultiple,
        @DecimalMin(value = "0", message = "{mes.valid.overIssueAllowanceCannotLess}")
        @DecimalMax(value = "100", message = "{mes.valid.overIssueAllowanceCannotExceed}") BigDecimal overIssueRate,
        @Size(max = 64, message = "{mes.valid.plannerCannotExceedCharacters}") String planner,
        Integer fqcRequired,
        @Size(max = 20, message = "{mes.valid.mostSubstitutes}") List<@Valid @NotNull SubstituteRequest> substitutes) {
}
