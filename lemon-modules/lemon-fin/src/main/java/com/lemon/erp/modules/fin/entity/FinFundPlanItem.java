package com.lemon.erp.modules.fin.entity;

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

    @NotNull(message = "请选择预计日期")
    private LocalDate planDate;

    @NotNull(message = "请选择方向")
    private FundDirection direction;

    @NotNull(message = "请填写金额")
    @DecimalMin(value = "0", inclusive = false, message = "金额必须大于 0")
    @Digits(integer = 16, fraction = 2, message = "金额最多2位小数")
    private BigDecimal amount;

    @NotBlank(message = "请填写说明")
    @Size(max = 128, message = "说明不能超过128个字符")
    private String description;

    private Long createBy;
}
