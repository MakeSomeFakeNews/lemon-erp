package com.lemon.erp.modules.fin.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.fin.model.FundDirection;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 资金计划的手工计划项（预计的其他收入、支出）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_fund_plan_item")
public class FinFundPlanItem extends BaseEntity {

    @NotNull(message = "{fin.valid.selectExpectedDate}")
    private LocalDate planDate;

    @NotNull(message = "{fin.valid.selectDirection}")
    private FundDirection direction;

    @NotNull(message = "{fin.valid.enterAmount}")
    @DecimalMin(value = "0", inclusive = false, message = "{fin.valid.amountMustGreater2}")
    @Digits(integer = 16, fraction = 2, message = "{fin.valid.amountAllowsMostDecimalPlaces}")
    private BigDecimal amount;

    @NotBlank(message = "{fin.valid.enterDescription}")
    @Size(max = 128, message = "{fin.valid.descriptionCannotExceedCharacters3}")
    private String description;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;
}
