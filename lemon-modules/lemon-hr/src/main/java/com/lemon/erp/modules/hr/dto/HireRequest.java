package com.lemon.erp.modules.hr.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 录用入职：按应聘者信息生成员工档案（试用期）
 *
 * @param empType       用工类型（字典 hr_emp_type），为空时为全职
 * @param regularDate   预计转正日期，为空时按入职日期 + 3 个月
 * @param baseSalary    试用期基本工资，填写时同时建立薪资档案
 * @param userId        关联已有的登录账号
 * @param username      为空且没有关联账号时不开通；填写时开通登录账号（初始密码见配置 lemon.user.initial-password）
 */
public record HireRequest(
        @NotBlank(message = "{hr.valid.enterEmployeeNo}")
        @Pattern(regexp = "^[A-Za-z0-9_-]{1,32}$", message = "{hr.valid.employeeNoMayOnlyContainLetters}") String empNo,
        @NotNull(message = "{hr.valid.selectHireDate}") LocalDate entryDate,
        Long positionId,
        String empType,
        LocalDate regularDate,
        @DecimalMin(value = "0", message = "{hr.valid.baseSalaryCannotLess}") @Digits(integer = 16, fraction = 2, message = "{hr.valid.amountAllowsMostDecimalPlaces}") BigDecimal baseSalary,
        Long userId,
        @Pattern(regexp = "^[A-Za-z0-9_.-]{2,30}$", message = "{hr.valid.loginNameMustLettersDigitsUnderscores}") String username) {
}
