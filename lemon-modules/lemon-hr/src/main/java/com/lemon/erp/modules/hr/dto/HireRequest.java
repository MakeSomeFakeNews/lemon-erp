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
        @NotBlank(message = "请填写工号")
        @Pattern(regexp = "^[A-Za-z0-9_-]{1,32}$", message = "工号只能包含字母、数字、下划线和短横线，最多32个字符") String empNo,
        @NotNull(message = "请选择入职日期") LocalDate entryDate,
        Long positionId,
        String empType,
        LocalDate regularDate,
        @DecimalMin(value = "0", message = "基本工资不能小于0") @Digits(integer = 16, fraction = 2, message = "金额最多2位小数") BigDecimal baseSalary,
        Long userId,
        @Pattern(regexp = "^[A-Za-z0-9_.-]{2,30}$", message = "登录名为2-30位字母、数字、下划线、点或短横线") String username) {
}
