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
        @NotNull(message = "请填写基本工资") @DecimalMin(value = "0", message = "基本工资不能小于0") @Digits(integer = 16, fraction = 2, message = "金额最多2位小数") BigDecimal baseSalary,
        @DecimalMin(value = "0", message = "岗位津贴不能小于0") @Digits(integer = 16, fraction = 2, message = "金额最多2位小数") BigDecimal postAllowance,
        @DecimalMin(value = "0", message = "其他补贴不能小于0") @Digits(integer = 16, fraction = 2, message = "金额最多2位小数") BigDecimal otherAllowance,
        @DecimalMin(value = "0", message = "绩效奖金基数不能小于0") @Digits(integer = 16, fraction = 2, message = "金额最多2位小数") BigDecimal perfBonusBase,
        @DecimalMin(value = "0", message = "社保基数不能小于0") @Digits(integer = 16, fraction = 2, message = "金额最多2位小数") BigDecimal socialBase,
        @DecimalMin(value = "0", message = "公积金基数不能小于0") @Digits(integer = 16, fraction = 2, message = "金额最多2位小数") BigDecimal housingBase,
        @DecimalMin(value = "5", message = "公积金比例为5%-12%") @DecimalMax(value = "12", message = "公积金比例为5%-12%") BigDecimal housingRate,
        @Size(max = 128, message = "开户行不能超过128个字符") String bankName,
        @Size(max = 64, message = "银行账号不能超过64个字符") String bankAccount,
        @Size(max = 255, message = "备注不能超过255个字符") String remark,
        LocalDate effectiveDate,
        Long schemeId) {

    public SalaryRequest(BigDecimal baseSalary, BigDecimal postAllowance, BigDecimal otherAllowance, BigDecimal perfBonusBase,
                         BigDecimal socialBase, BigDecimal housingBase, BigDecimal housingRate, String bankName, String bankAccount,
                         String remark, LocalDate effectiveDate) {
        this(baseSalary, postAllowance, otherAllowance, perfBonusBase, socialBase, housingBase, housingRate, bankName, bankAccount,
                remark, effectiveDate, null);
    }
}
