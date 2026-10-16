package com.lemon.erp.modules.fin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 费用报销（草稿）
 *
 * @param userId 报销人，为空时为当前用户
 * @param deptId 费用部门，为空时取报销人的部门
 * @param loanId 付款时冲抵的借款（选填）
 */
public record ClaimRequest(
        Long userId,
        Long deptId,
        @NotNull(message = "请选择报销日期") LocalDate claimDate,
        Long loanId,
        @Size(max = 255, message = "事由不能超过255个字符") String remark,
        @NotEmpty(message = "请添加报销明细") @Size(max = 100, message = "明细不能超过100行") List<@Valid @NotNull Item> items) {

    public record Item(
            LocalDate expenseDate,
            @NotNull(message = "请选择费用科目") Long accountId,
            Long projectId,
            @NotNull(message = "请填写金额") @DecimalMin(value = "0.01", message = "金额必须大于0")
            @Digits(integer = 16, fraction = 2, message = "金额最多2位小数") BigDecimal amount,
            @Size(max = 255, message = "说明不能超过255个字符") String remark) {
    }
}
