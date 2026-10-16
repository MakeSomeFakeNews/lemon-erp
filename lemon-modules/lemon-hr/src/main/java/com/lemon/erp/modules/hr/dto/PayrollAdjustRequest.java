package com.lemon.erp.modules.hr.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 工资明细手工调整：其他奖金、其他扣款，调整后重新计算个税和实发
 */
public record PayrollAdjustRequest(
        @DecimalMin(value = "0", message = "其他奖金不能小于0") @Digits(integer = 16, fraction = 2, message = "金额最多2位小数") BigDecimal otherBonus,
        @DecimalMin(value = "0", message = "其他扣款不能小于0") @Digits(integer = 16, fraction = 2, message = "金额最多2位小数") BigDecimal otherDeduct,
        @Size(max = 255, message = "备注不能超过255个字符") String remark) {
}
