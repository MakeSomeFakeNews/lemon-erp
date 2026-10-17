package com.lemon.erp.modules.hr.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 工资明细手工调整：其他奖金、其他扣款，调整后重新计算个税和实发
 */
public record PayrollAdjustRequest(
        @DecimalMin(value = "0", message = "{hr.valid.otherBonusCannotLess}") @Digits(integer = 16, fraction = 2, message = "{hr.valid.amountAllowsMostDecimalPlaces}") BigDecimal otherBonus,
        @DecimalMin(value = "0", message = "{hr.valid.otherDeductionsCannotLess}") @Digits(integer = 16, fraction = 2, message = "{hr.valid.amountAllowsMostDecimalPlaces}") BigDecimal otherDeduct,
        @Size(max = 255, message = "{hr.valid.remarkCannotExceedCharacters}") String remark) {
}
