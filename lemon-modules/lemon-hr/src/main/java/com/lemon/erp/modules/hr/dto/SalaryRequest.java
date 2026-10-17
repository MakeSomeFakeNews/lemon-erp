package com.lemon.erp.modules.hr.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 薪资档案；修改基本工资时记录一条调薪异动
 */
/**
 * @param effectiveDate 生效日期：新建档案默认入职日期，调整默认当天；晚于今天的调整到期后计入工资
 */
public record SalaryRequest(
        @NotNull(message = "{hr.valid.enterBaseSalary}") @DecimalMin(value = "0", message = "{hr.valid.baseSalaryCannotLess}") @Digits(integer = 16, fraction = 2, message = "{hr.valid.amountAllowsMostDecimalPlaces}") BigDecimal baseSalary,
        @DecimalMin(value = "0", message = "{hr.valid.positionAllowanceCannotLess}") @Digits(integer = 16, fraction = 2, message = "{hr.valid.amountAllowsMostDecimalPlaces}") BigDecimal postAllowance,
        @DecimalMin(value = "0", message = "{hr.valid.otherAllowancesCannotLess}") @Digits(integer = 16, fraction = 2, message = "{hr.valid.amountAllowsMostDecimalPlaces}") BigDecimal otherAllowance,
        @DecimalMin(value = "0", message = "{hr.valid.performanceBonusBaseCannotLess}") @Digits(integer = 16, fraction = 2, message = "{hr.valid.amountAllowsMostDecimalPlaces}") BigDecimal perfBonusBase,
        @DecimalMin(value = "0", message = "{hr.valid.socialInsuranceBaseCannotLess}") @Digits(integer = 16, fraction = 2, message = "{hr.valid.amountAllowsMostDecimalPlaces}") BigDecimal socialBase,
        @DecimalMin(value = "0", message = "{hr.valid.housingFundBaseCannotLess}") @Digits(integer = 16, fraction = 2, message = "{hr.valid.amountAllowsMostDecimalPlaces}") BigDecimal housingBase,
        @DecimalMin(value = "5", message = "{hr.valid.housingFundRateMust}") @DecimalMax(value = "12", message = "{hr.valid.housingFundRateMust}") BigDecimal housingRate,
        @Size(max = 128, message = "{hr.valid.bankNameCannotExceedCharacters}") String bankName,
        @Size(max = 64, message = "{hr.valid.bankAccountCannotExceedCharacters}") String bankAccount,
        @Size(max = 255, message = "{hr.valid.remarkCannotExceedCharacters}") String remark,
        LocalDate effectiveDate,
        Long schemeId) {

    public SalaryRequest(BigDecimal baseSalary, BigDecimal postAllowance, BigDecimal otherAllowance, BigDecimal perfBonusBase,
                         BigDecimal socialBase, BigDecimal housingBase, BigDecimal housingRate, String bankName, String bankAccount,
                         String remark, LocalDate effectiveDate) {
        this(baseSalary, postAllowance, otherAllowance, perfBonusBase, socialBase, housingBase, housingRate, bankName, bankAccount,
                remark, effectiveDate, null);
    }
}
