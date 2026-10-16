package com.lemon.erp.modules.fin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 凭证分录：借方、贷方只能填一边
 *
 * 辅助核算（部门、项目、客户、供应商、存货）可同时填写，科目上设置了的必填
 *
 * @param accountId 末级科目
 */
public record VoucherEntryRequest(
        @NotBlank(message = "请填写摘要") @Size(max = 255, message = "摘要不能超过255个字符") String summary,
        @NotNull(message = "请选择科目") Long accountId,
        Long deptId,
        @DecimalMin(value = "0", message = "金额不能小于0") @Digits(integer = 16, fraction = 2, message = "金额最多2位小数") BigDecimal debit,
        @DecimalMin(value = "0", message = "金额不能小于0") @Digits(integer = 16, fraction = 2, message = "金额最多2位小数") BigDecimal credit,
        Long projectId,
        Long customerId,
        Long supplierId,
        Long materialId) {
}
