package com.lemon.erp.modules.scm.dto;

import com.lemon.erp.modules.scm.model.RebateSettle;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 新增 / 修改返利协议（执行中）
 *
 * @param tiers 阶梯：销售额达到 minAmount 时返利率为 rate%，取达到的最高一档
 */
public record RebateRequest(
        @NotNull(message = "请选择客户") Long customerId,
        @NotNull(message = "请选择开始日期") LocalDate beginDate,
        @NotNull(message = "请选择结束日期") LocalDate endDate,
        @NotNull(message = "请选择兑付方式") RebateSettle settleMode,
        @NotEmpty(message = "请添加返利阶梯") @Size(max = 20, message = "阶梯不能超过20档") List<@Valid @NotNull Tier> tiers,
        @Size(max = 255, message = "说明不能超过255个字符") String remark) {

    public record Tier(
            @NotNull(message = "请填写销售额") @DecimalMin(value = "0", message = "销售额不能为负数") @Digits(integer = 16, fraction = 2) BigDecimal minAmount,
            @NotNull(message = "请填写返利率") @DecimalMin(value = "0", inclusive = false, message = "返利率须大于0")
            @DecimalMax(value = "100", message = "返利率不能超过100") BigDecimal rate) {
    }
}
