package com.lemon.erp.modules.fin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 固定资产处置
 *
 * @param income          处置收入（变卖收到的款项），可为空
 * @param expense         清理费用，可为空
 * @param settleAccountId 收付款的银行 / 现金科目，有收入或费用时必填
 */
public record DisposeRequest(
        LocalDate date,
        @DecimalMin(value = "0", message = "{fin.valid.disposalProceedsCannotLess}") @Digits(integer = 16, fraction = 2, message = "{fin.valid.amountAllowsMostDecimalPlaces}") BigDecimal income,
        @DecimalMin(value = "0", message = "{fin.valid.disposalExpensesCannotLess}") @Digits(integer = 16, fraction = 2, message = "{fin.valid.amountAllowsMostDecimalPlaces}") BigDecimal expense,
        Long settleAccountId,
        @Size(max = 255, message = "{fin.valid.descriptionCannotExceedCharacters}") String remark) {
}
